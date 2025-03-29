import en from './en.json';
import es from './es.json';

export type Language = 'en' | 'es';
export const defaultLanguage: Language = 'en';

export const translations = {
  en,
  es,
};
