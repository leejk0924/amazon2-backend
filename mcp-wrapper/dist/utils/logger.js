"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.logger = void 0;
const LOG_LEVEL = process.env.LOG_LEVEL || 'info';
const LEVELS = { debug: 0, info: 1, warn: 2, error: 3 };
const currentLevel = LEVELS[LOG_LEVEL] || 1;
exports.logger = {
    info(message) {
        if (currentLevel <= LEVELS.info) {
            console.log(`[INFO] ${new Date().toISOString()} - ${message}`);
        }
    },
    error(message, error) {
        if (currentLevel <= LEVELS.error) {
            console.error(`[ERROR] ${new Date().toISOString()} - ${message}`);
            if (error) {
                console.error(error.stack);
            }
        }
    },
    debug(message) {
        if (currentLevel <= LEVELS.debug) {
            console.log(`[DEBUG] ${new Date().toISOString()} - ${message}`);
        }
    },
    warn(message) {
        if (currentLevel <= LEVELS.warn) {
            console.warn(`[WARN] ${new Date().toISOString()} - ${message}`);
        }
    }
};
//# sourceMappingURL=logger.js.map