import { isGamePage } from './environment.js';

// Lite build: performance-first game settings for low-end phones.
//
// These are the game's own options (Settings -> Performance / Effects / Players /
// Display / Map nodes), written once into the game's local settings before it
// starts, exactly as if the player had changed them by hand. They are stored only
// on this device and are never sent to the game server. Anything the player
// changes afterwards in Settings is kept; this runs a single time per install.
const MARKER = 'sw_lite_defaults_v1';
const SETTINGS_KEY = 'scapewatch_settings';

const LITE_SETTINGS = {
  // Performance
  frameCap: 30, // lowest value the game allows (default 60)
  // Weather and lighting effects (particles + full-screen tints)
  effectDayNight: false,
  effectSnow: false,
  effectDesert: false,
  effectWooded: false,
  effectVolcanic: false,
  effectShadow: false,
  effectWind: false,
  mapShowWeather: false,
  // Fewer sprites and labels to draw on the world map
  maxVisibleStrangers: 5, // default 25; friends and clanmates are still shown
  showOtherPlayerPets: false,
  showOtherPlayerNames: false,
  ghostIdlePlayers: false,
  bankNodeTextBackground: 'off',
  skillNodeTextBackground: 'off',
  combatNodeTextBackground: 'off',
  // Fewer animations
  showLootDrops: false, // spinning item icons
  showLuckXPDrops: false, // can fire on every XP gain
  playerPathAnimation: 'static',
  notificationBackground: false,
  // Audio decoding costs CPU; stop music while the app is in the background
  musicPauseHidden: true,
};

export function applyLiteDefaults() {
  if (!isGamePage()) return;
  try {
    if (localStorage.getItem(MARKER)) return;
    let current = {};
    try {
      current = JSON.parse(localStorage.getItem(SETTINGS_KEY) || '{}') || {};
    } catch (_) {
      current = {};
    }
    localStorage.setItem(SETTINGS_KEY, JSON.stringify({ ...current, ...LITE_SETTINGS }));
    localStorage.setItem(MARKER, '1');
  } catch (_) {
    // Storage unavailable: the game simply starts with its own defaults.
  }
}
