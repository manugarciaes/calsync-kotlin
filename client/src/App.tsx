import { Switch, Route } from "wouter";
import { queryClient } from "./lib/queryClient";
import { QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "@/components/ui/toaster";
import NotFound from "@/pages/not-found";
import AuthPage from "@/pages/auth-page";
import DashboardPage from "@/pages/dashboard-page";
import CalendarPage from "@/pages/calendar-page";
import FreeSlotsPage from "@/pages/free-slots-page";
import SettingsPage from "@/pages/settings-page";
import PublicCalendarPage from "@/pages/public-calendar-page";
import { ProtectedRoute } from "./lib/protected-route";
import { AuthProvider } from "./hooks/use-auth";
import { TranslationsProvider } from "./hooks/use-translations";
import { CalendarsProvider } from "./hooks/use-calendars";
import { FreeSlotsProvider } from "./hooks/use-free-slots";

function Router() {
  return (
    <Switch>
      <ProtectedRoute path="/" component={DashboardPage} />
      <ProtectedRoute path="/calendar" component={CalendarPage} />
      <ProtectedRoute path="/free-slots" component={FreeSlotsPage} />
      <ProtectedRoute path="/settings" component={SettingsPage} />
      <Route path="/auth" component={AuthPage} />
      <Route path="/s/:shareUrl" component={PublicCalendarPage} />
      <Route component={NotFound} />
    </Switch>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <TranslationsProvider>
        <AuthProvider>
          <CalendarsProvider>
            <FreeSlotsProvider>
              <Router />
              <Toaster />
            </FreeSlotsProvider>
          </CalendarsProvider>
        </AuthProvider>
      </TranslationsProvider>
    </QueryClientProvider>
  );
}

export default App;
