import { defineStore } from "pinia";
import axiosInstanse from "src/services/api";

export const useAuthStore = defineStore('auth', {
    state: () => ({
        accessToken: null as string | null,
        user: null as null | {
            id: string;
            name: string;
            email: string;
            technologies: string[];
        },
        isLoading: false,
    }),

    getters: {
        isAuthenticated: (state) => !!state.accessToken
    },

    actions: {
        async login(email: string, password: string) {
            try {
                this.isLoading = true;

                const response = await axiosInstanse.post(
                    "/api/auth/login",
                    { email, password }
                )
                this.accessToken = response.data.accessToken;
                if (this.accessToken === null) {
                    throw new Error("Failed to get access token");
                }
                localStorage.setItem("token", this.accessToken);

                const userResponse = await axiosInstanse.get('/api/user/profile')
                const p = userResponse.data
                this.user = { id: p.id, name: p.username, email: p.email, technologies: p.technologies ?? [] }

            } catch (err) {
                console.log("Login failed:", err);
                throw err;
            } finally {
                this.isLoading = false;
            }
        },

        logout() {
            this.accessToken = null;
            this.user = null;

            localStorage.removeItem("token");
        },
        
        async register(username: string, email: string, password: string, 
            technologies: string[]) {
            try {
                this.isLoading = true;

                const response = await axiosInstanse.post(
                    "/api/auth/register",
                    { username, email, password }
                );
                this.accessToken = response.data.accessToken;
                if (this.accessToken === null) {
                    throw new Error("Failed to get access token");
                }
                localStorage.setItem("token", this.accessToken);
                const rp = response.data
                this.user = { id: rp.user?.id ?? rp.id, name: rp.user?.username ?? rp.username, email: rp.user?.email ?? rp.email, technologies: [] }

                await axiosInstanse.put(
                    "/api/user/technologies",
                    technologies
                )

            } catch (err) {
                console.log("Registration failed:", err);
                throw err;
            } finally {
                this.isLoading = false;
            }
        }
    }
})