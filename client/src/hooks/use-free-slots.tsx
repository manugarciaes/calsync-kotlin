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
interface KotlinFreeSlot {
  id: string;
  userId: string;
  name: string;
  description: string | null;
  duration: number;
  daysInAdvance: number;
  startTime: string;
  endTime: string;
  daysOfWeek: number[];
  calendarIds: string[];
  shareUrl: string;
  createdAt: string;
  updatedAt: string;
}

interface KotlinBooking {
  id: string;
  freeSlotId: string;
  name: string;
  email: string;
  date: string;
  startTime: string;
  endTime: string;
  status: string;
  notes: string | null;
  createdAt: string;
}

interface CreateFreeSlotDto {
  name: string;
  description?: string;
  duration: number;
  daysInAdvance: number;
  startTime: string;
  endTime: string;
  daysOfWeek: number[];
  calendarIds: string[];
}

interface AvailableTimeSlot {
  date: string;
  slots: {
    start: string;
    end: string;
  }[];
}

type FreeSlotsContextType = {
  freeSlots: KotlinFreeSlot[];
  isLoading: boolean;
  error: Error | null;
  createFreeSlotMutation: UseMutationResult<KotlinFreeSlot, Error, CreateFreeSlotDto>;
  updateFreeSlotMutation: UseMutationResult<KotlinFreeSlot, Error, {id: string, updates: Partial<CreateFreeSlotDto>}>;
  deleteFreeSlotMutation: UseMutationResult<void, Error, string>;
  getFreeSlot: (id: string) => Promise<KotlinFreeSlot>;
  getBookings: (freeSlotId: string) => Promise<KotlinBooking[]>;
  getPublicFreeSlot: (shareUrl: string) => Promise<KotlinFreeSlot>;
  getAvailableTimes: (shareUrl: string, date: string) => Promise<AvailableTimeSlot>;
  bookFreeSlotMutation: UseMutationResult<KotlinBooking, Error, BookFreeSlotDto>;
  updateBookingStatusMutation: UseMutationResult<KotlinBooking, Error, {bookingId: string, status: string}>;
};

interface BookFreeSlotDto {
  shareUrl: string;
  name: string;
  email: string;
  date: string;
  startTime: string;
  endTime: string;
  notes?: string;
}

export const FreeSlotsContext = createContext<FreeSlotsContextType | null>(null);

export function FreeSlotsProvider({ children }: { children: ReactNode }) {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  
  const {
    data: freeSlots = [],
    error,
    isLoading,
  } = useQuery<KotlinFreeSlot[], Error>({
    queryKey: ["/free-slots"],
    retry: false,
  });

  // Function to get a single free slot by ID
  const getFreeSlot = async (id: string): Promise<KotlinFreeSlot> => {
    const res = await apiRequest("GET", `/free-slots/${id}`);
    return await res.json();
  };

  // Function to get bookings for a free slot
  const getBookings = async (freeSlotId: string): Promise<KotlinBooking[]> => {
    const res = await apiRequest("GET", `/bookings/${freeSlotId}`);
    return await res.json();
  };

  // Function to get a public free slot by share URL
  const getPublicFreeSlot = async (shareUrl: string): Promise<KotlinFreeSlot> => {
    const res = await fetch(`/api/public/slots/${shareUrl}`, {
      method: "GET",
    });
    
    if (!res.ok) {
      const errText = await res.text();
      throw new Error(errText);
    }
    
    return await res.json();
  };

  // Function to get available times for a free slot on a specific date
  const getAvailableTimes = async (shareUrl: string, date: string): Promise<AvailableTimeSlot> => {
    const res = await fetch(`/api/public/slots/${shareUrl}/available-times?date=${date}`, {
      method: "GET",
    });
    
    if (!res.ok) {
      const errText = await res.text();
      throw new Error(errText);
    }
    
    return await res.json();
  };

  const createFreeSlotMutation = useMutation({
    mutationFn: async (freeSlot: CreateFreeSlotDto) => {
      const res = await apiRequest("POST", "/free-slots", freeSlot);
      return await res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["/free-slots"] });
      toast({
        title: "Free slot created",
        description: "Your free slot has been created successfully",
      });
    },
    onError: (error: Error) => {
      toast({
        title: "Failed to create free slot",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const updateFreeSlotMutation = useMutation({
    mutationFn: async ({ id, updates }: {id: string, updates: Partial<CreateFreeSlotDto>}) => {
      const res = await apiRequest("PUT", `/free-slots/${id}`, updates);
      return await res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["/free-slots"] });
      toast({
        title: "Free slot updated",
        description: "Your free slot has been updated successfully",
      });
    },
    onError: (error: Error) => {
      toast({
        title: "Failed to update free slot",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const deleteFreeSlotMutation = useMutation({
    mutationFn: async (id: string) => {
      await apiRequest("DELETE", `/free-slots/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["/free-slots"] });
      toast({
        title: "Free slot deleted",
        description: "Your free slot has been deleted successfully",
      });
    },
    onError: (error: Error) => {
      toast({
        title: "Failed to delete free slot",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const bookFreeSlotMutation = useMutation({
    mutationFn: async (bookingData: BookFreeSlotDto) => {
      const res = await fetch("/api/public/book", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(bookingData),
      });
      
      if (!res.ok) {
        const errText = await res.text();
        throw new Error(errText);
      }
      
      return await res.json();
    },
    onSuccess: () => {
      toast({
        title: "Booking successful",
        description: "Your booking has been confirmed. You will receive a confirmation email shortly.",
      });
    },
    onError: (error: Error) => {
      toast({
        title: "Booking failed",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  const updateBookingStatusMutation = useMutation({
    mutationFn: async ({ bookingId, status }: {bookingId: string, status: string}) => {
      const res = await apiRequest("PUT", `/bookings/${bookingId}/status`, { status });
      return await res.json();
    },
    onSuccess: (booking) => {
      queryClient.invalidateQueries({ queryKey: [`/bookings/${booking.freeSlotId}`] });
      toast({
        title: "Booking status updated",
        description: "The booking status has been updated successfully",
      });
    },
    onError: (error: Error) => {
      toast({
        title: "Failed to update booking status",
        description: error.message,
        variant: "destructive",
      });
    },
  });

  return (
    <FreeSlotsContext.Provider
      value={{
        freeSlots,
        isLoading,
        error,
        createFreeSlotMutation,
        updateFreeSlotMutation,
        deleteFreeSlotMutation,
        getFreeSlot,
        getBookings,
        getPublicFreeSlot,
        getAvailableTimes,
        bookFreeSlotMutation,
        updateBookingStatusMutation,
      }}
    >
      {children}
    </FreeSlotsContext.Provider>
  );
}

export function useFreeSlots() {
  const context = useContext(FreeSlotsContext);
  if (!context) {
    throw new Error("useFreeSlots must be used within a FreeSlotsProvider");
  }
  return context;
}