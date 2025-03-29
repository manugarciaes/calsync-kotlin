import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { translations, defaultLanguage, type Language } from '../translations';

type TranslationsContextType = {
  t: (key: string, params?: Record<string, string>) => string;
  currentLanguage: Language;
  setLanguage: (lang: Language) => void;
};

const TranslationsContext = createContext<TranslationsContextType | null>(null);

export function TranslationsProvider({ children }: { children: ReactNode }) {
  // Get language from localStorage if available, or use browser language, or fall back to default
  const getBrowserLanguage = (): Language => {
    const browserLang = navigator.language.split('-')[0] as Language;
    return translations[browserLang] ? browserLang : defaultLanguage;
  };
  
  const [currentLanguage, setCurrentLanguage] = useState<Language>(() => {
    const storedLang = localStorage.getItem('language') as Language;
    return storedLang && translations[storedLang] ? storedLang : getBrowserLanguage();
  });

  useEffect(() => {
    localStorage.setItem('language', currentLanguage);
    document.documentElement.lang = currentLanguage;
  }, [currentLanguage]);

  const t = (key: string, params?: Record<string, string>): string => {
    const keys = key.split('.');
    let value: any = translations[currentLanguage];
    
    for (const k of keys) {
      if (!value[k]) {
        // Fallback to default language if key not found
        value = translations[defaultLanguage];
        for (const fallbackKey of keys) {
          if (!value[fallbackKey]) {
            return key; // Return key if not found in fallback
          }
          value = value[fallbackKey];
        }
        break;
      }
      value = value[k];
    }
    
    if (typeof value !== 'string') {
      return key;
    }
    
    // Replace parameters
    if (params) {
      return Object.entries(params).reduce(
        (str, [key, value]) => str.replace(new RegExp(`{{${key}}}`, 'g'), value),
        value
      );
    }
    
    return value;
  };

  const setLanguage = (lang: Language) => {
    if (translations[lang]) {
      setCurrentLanguage(lang);
    }
  };

  return (
    <TranslationsContext.Provider value={{ t, currentLanguage, setLanguage }}>
      {children}
    </TranslationsContext.Provider>
  );
}

export function useTranslations() {
  const context = useContext(TranslationsContext);
  if (!context) {
    throw new Error('useTranslations must be used within a TranslationsProvider');
  }
  return context;
}
