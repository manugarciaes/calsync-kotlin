import { createContext, ReactNode, useContext, useEffect } from "react";
import {
  useQuery,
  useMutation,
  UseMutationResult,
} from "@tanstack/react-query";
import { insertUserSchema } from "@shared/schema";
import { getQueryFn, apiRequest, queryClient, setAuthToken, removeAuthToken } from "../lib/queryClient";
import { useToast } from "@/hooks/use-toast";
import { z } from "zod";

// Define user type to match Kotlin backend response
interface KotlinUser {
  id: string;
  username: string;
  email: string;
  name: string;
  createdAt: string;
}

// Define auth response from Kotlin backend
interface AuthResponse {
  token: string;
  user: KotlinUser;
}

type AuthContextType = {
  user: KotlinUser | null;
  isLoading: boolean;
  error: Error | null;
  loginMutation: UseMutationResult<KotlinUser, Error, LoginData>;
  logoutMutation: UseMutationResult<void, Error, void>;
  registerMutation: UseMutationResult<KotlinUser, Error, RegisterData>;
};

// Extend the insertUserSchema for registration
const registerSchema = insertUserSchema.extend({
  confirmPassword: z.string(),
}).refine(data => data.password === data.confirmPassword, {
  message: "Passwords don't match",
  path: ["confirmPassword"],
});

type LoginData = {
  username: string;
  password: string;
};

type RegisterData = z.infer<typeof registerSchema>;

export const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const { toast } = useToast();
  
  const {
    data: user,
    error,
    isLoading,
    refetch
  } = useQuery<KotlinUser | undefined, Error>({
    queryKey: ["/user"],
    queryFn: getQueryFn({ on401: "returnNull" }),
  });

  // Check for token on mount
  useEffect(() => {
    // If we have a token but no user, refetch user data
    if (localStorage.getItem("auth_token") && !user) {
      refetch();
    }
  }, [refetch]);

  const loginMutation = useMutation({
    mutationFn: async (credentials: LoginData) => {
      const res = await apiRequest("POST", "/login", credentials);
      const authResponse = await res.json() as AuthResponse;
      
      // Store the JWT token
      setAuthToken(authResponse.token);
      
      return authResponse.user;
    },
    onSuccess: (user: KotlinUser) => {
      queryClient.setQueryData(["/user"], user);
      toast({
        title: "Login successful",
        description: "Welcome back!",
      });
    },
    onError: (error: Error) => {
      toast({
        title: "Login failed",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const registerMutation = useMutation({
    mutationFn: async (credentials: RegisterData) => {
      // Remove confirmPassword before sending to API
      const { confirmPassword, ...userData } = credentials;
      const res = await apiRequest("POST", "/register", userData);
      const authResponse = await res.json() as AuthResponse;
      
      // Store the JWT token
      setAuthToken(authResponse.token);
      
      return authResponse.user;
    },
    onSuccess: (user: KotlinUser) => {
      queryClient.setQueryData(["/user"], user);
      toast({
        title: "Registration successful",
        description: "Welcome to CalSync!",
      });
    },
    onError: (error: Error) => {
      toast({
        title: "Registration failed",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const logoutMutation = useMutation({
    mutationFn: async () => {
      // No need to call the backend for JWT logout
      // Just remove the token from local storage
      removeAuthToken();
    },
    onSuccess: () => {
      queryClient.setQueryData(["/user"], null);
      // Invalidate all queries to force refetch when logging back in
      queryClient.invalidateQueries();
      
      toast({
        title: "Logged out",
        description: "You have been logged out successfully",
      });
    },
    onError: (error: Error) => {
      toast({
        title: "Logout failed",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  return (
    <AuthContext.Provider
      value={{
        user: user ?? null,
        isLoading,
        error,
        loginMutation,
        logoutMutation,
        registerMutation,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
