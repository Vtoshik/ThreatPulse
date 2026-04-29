import { defineStore } from "pinia";
import axiosInstanse from "src/services/api";

export const useAuthStore = defineStore('auth', {
    state: () => ({
        accessToken: null as string | null,
        user: null as null | {
            id: string;
            name: string;
            email: string;
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

                const userResponse = await axiosInstanse.get(
                    "/api/user/profile");
                this.user = userResponse.data.user;

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
                this.user = response.data.user;

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