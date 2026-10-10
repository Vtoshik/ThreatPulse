import { defineStore } from "pinia";
import api from "src/services/api";

export interface AuthConfig {
    registrationEnabled: boolean;
    demoEnabled: boolean;
    credentialsChangeEnabled: boolean;
    emailAlertsAvailable: boolean;
}

interface Profile {
    id: number;
    username: string;
    technologies?: string[];
}

// Until the server answers, show nothing that might be rejected
const DEFAULT_CONFIG: AuthConfig = {
    registrationEnabled: false,
    demoEnabled: false,
    credentialsChangeEnabled: false,
    emailAlertsAvailable: false,
};

export const useAuthStore = defineStore('auth', {
    state: () => ({
        accessToken: null as string | null,
        user: null as null | {
            id: string;
            name: string;
            technologies: string[];
        },
        config: { ...DEFAULT_CONFIG } as AuthConfig,
        configLoaded: false,
        isLoading: false,
    }),

    getters: {
        isAuthenticated: (state) => !!state.accessToken
    },

    actions: {
        async loadConfig() {
            if (this.configLoaded) return;
            try {
                const response = await api.get<AuthConfig>('/api/auth/config');
                this.config = response.data;
                this.configLoaded = true;
            } catch {
                // The server is unreachable: keep the safe defaults, the next call tries again
            }
        },

        async loadProfile() {
            const response = await api.get<Profile>('/api/user/profile');
            const p = response.data;
            this.user = {
                id: String(p.id),
                name: p.username,
                technologies: p.technologies ?? [],
            };
        },

        async login(username: string, password: string) {
            try {
                this.isLoading = true;
                const response = await api.post("/api/auth/login", { username, password });
                this.setToken(response.data.accessToken);
                await this.loadProfile();
            } finally {
                this.isLoading = false;
            }
        },

        async loginDemo() {
            try {
                this.isLoading = true;
                const response = await api.post('/api/auth/demo');
                this.setToken(response.data.accessToken);
                await this.loadProfile();
            } finally {
                this.isLoading = false;
            }
        },

        async register(username: string, password: string, technologies: string[]) {
            try {
                this.isLoading = true;
                const response = await api.post("/api/auth/register", { username, password });
                this.setToken(response.data.accessToken);
                await api.put("/api/user/technologies", technologies);
                await this.loadProfile();
            } finally {
                this.isLoading = false;
            }
        },

        async changeCredentials(currentPassword: string, newUsername?: string, newPassword?: string) {
            const response = await api.put('/api/user/credentials', {
                currentPassword,
                newUsername: newUsername || undefined,
                newPassword: newPassword || undefined,
            });
            // A new password invalidates every earlier token, including the one in use,
            // so the server answers with a fresh one
            this.setToken(response.data.accessToken);
            await this.loadProfile();
        },

        async restoreSession() {
            const token = localStorage.getItem("token");
            if (!token || this.user) return;
            this.accessToken = token;
            try {
                await this.loadProfile();
            } catch {
                this.accessToken = null;
                localStorage.removeItem("token");
            }
        },

        logout() {
            this.accessToken = null;
            this.user = null;
            localStorage.removeItem("token");
        },

        setToken(token: unknown) {
            if (typeof token !== 'string' || token === '') {
                throw new Error("Failed to get access token");
            }
            this.accessToken = token;
            localStorage.setItem("token", token);
        },
    }
})
