import { createContext, ReactNode, useContext } from "react";
import {
  useQuery,
  useMutation,
  UseMutationResult,
  useQueryClient,
} from "@tanstack/react-query";
import { apiRequest } from "../lib/queryClient";
import { useToast } from "@/hooks/use-toast";

// Types to match Kotlin backend
interface KotlinCalendar {
  id: string;
  userId: string;
  name: string;
  source: string;
  sourceType: string;
  color: string;
  lastSynced: string | null;
  createdAt: string;
  updatedAt: string;
}

interface CreateCalendarDto {
  name: string;
  source: string;
  sourceType: string;
  color: string;
}

interface UploadCalendarFileDto {
  name: string;
  color: string;
  file: File;
}

type CalendarsContextType = {
  calendars: KotlinCalendar[];
  isLoading: boolean;
  error: Error | null;
  createCalendarMutation: UseMutationResult<KotlinCalendar, Error, CreateCalendarDto>;
  updateCalendarMutation: UseMutationResult<KotlinCalendar, Error, {id: string, updates: Partial<CreateCalendarDto>}>;
  deleteCalendarMutation: UseMutationResult<void, Error, string>;
  uploadCalendarFileMutation: UseMutationResult<KotlinCalendar, Error, UploadCalendarFileDto>;
  syncCalendarMutation: UseMutationResult<KotlinCalendar, Error, string>;
};

export const CalendarsContext = createContext<CalendarsContextType | null>(null);

export function CalendarsProvider({ children }: { children: ReactNode }) {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  
  const {
    data: calendars = [],
    error,
    isLoading,
  } = useQuery<KotlinCalendar[], Error>({
    queryKey: ["/calendars"],
    retry: false,
  });

  const createCalendarMutation = useMutation({
    mutationFn: async (calendar: CreateCalendarDto) => {
      const res = await apiRequest("POST", "/calendars", calendar);
      return await res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["/calendars"] });
      toast({
        title: "Calendar created",
        description: "Your calendar has been created successfully",
      });
    },
    onError: (error: Error) => {
      toast({
        title: "Failed to create calendar",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const updateCalendarMutation = useMutation({
    mutationFn: async ({ id, updates }: {id: string, updates: Partial<CreateCalendarDto>}) => {
      const res = await apiRequest("PUT", `/calendars/${id}`, updates);
      return await res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["/calendars"] });
      toast({
        title: "Calendar updated",
        description: "Your calendar has been updated successfully",
      });
    },
    onError: (error: Error) => {
      toast({
        title: "Failed to update calendar",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const deleteCalendarMutation = useMutation({
    mutationFn: async (id: string) => {
      await apiRequest("DELETE", `/calendars/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["/calendars"] });
      toast({
        title: "Calendar deleted",
        description: "Your calendar has been deleted successfully",
      });
    },
    onError: (error: Error) => {
      toast({
        title: "Failed to delete calendar",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const uploadCalendarFileMutation = useMutation({
    mutationFn: async ({ name, color, file }: UploadCalendarFileDto) => {
      const formData = new FormData();
      formData.append("name", name);
      formData.append("color", color);
      formData.append("file", file);
      
      // Use fetch directly since we need to use FormData
      const res = await fetch("/api/calendars/upload", {
        method: "POST",
        body: formData,
        headers: {
          "Authorization": `Bearer ${localStorage.getItem("auth_token")}`
        }
      });
      
      if (!res.ok) {
        const errText = await res.text();
        throw new Error(errText);
      }
      
      return await res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["/calendars"] });
      toast({
        title: "Calendar uploaded",
        description: "Your calendar file has been uploaded successfully",
      });
    },
    onError: (error: Error) => {
      toast({
        title: "Failed to upload calendar",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const syncCalendarMutation = useMutation({
    mutationFn: async (id: string) => {
      const res = await apiRequest("POST", `/calendars/${id}/sync`);
      return await res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["/calendars"] });
      toast({
        title: "Calendar synced",
        description: "Your calendar has been synced successfully",
      });
    },
    onError: (error: Error) => {
      toast({
        title: "Failed to sync calendar",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  return (
    <CalendarsContext.Provider
      value={{
        calendars,
        isLoading,
        error,
        createCalendarMutation,
        updateCalendarMutation,
        deleteCalendarMutation,
        uploadCalendarFileMutation,
        syncCalendarMutation,
      }}
    >
      {children}
    </CalendarsContext.Provider>
  );
}

export function useCalendars() {
  const context = useContext(CalendarsContext);
  if (!context) {
    throw new Error("useCalendars must be used within a CalendarsProvider");
  }
  return context;
}