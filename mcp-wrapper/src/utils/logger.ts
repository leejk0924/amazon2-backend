import { Logger } from '../types';

const LOG_LEVEL = process.env.LOG_LEVEL || 'info';
const LEVELS = { debug: 0, info: 1, warn: 2, error: 3 };
const currentLevel = LEVELS[LOG_LEVEL as keyof typeof LEVELS] || 1;

export const logger: Logger = {
  info(message: string) {
    if (currentLevel <= LEVELS.info) {
      console.log(`[INFO] ${new Date().toISOString()} - ${message}`);
    }
  },

  error(message: string, error?: Error) {
    if (currentLevel <= LEVELS.error) {
      console.error(`[ERROR] ${new Date().toISOString()} - ${message}`);
      if (error) {
        console.error(error.stack);
      }
    }
  },

  debug(message: string) {
    if (currentLevel <= LEVELS.debug) {
      console.log(`[DEBUG] ${new Date().toISOString()} - ${message}`);
    }
  },

  warn(message: string) {
    if (currentLevel <= LEVELS.warn) {
      console.warn(`[WARN] ${new Date().toISOString()} - ${message}`);
    }
  }
};
