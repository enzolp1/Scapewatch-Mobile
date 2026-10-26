import styles from './styles/index.css';
import { prepareGameLayout } from './game-layout.js';
import { initializeRenderer } from './mobile-renderer.js';
import { initializeMobileUi } from './ui/index.js';
import { applyLiteDefaults } from './lite-defaults.js';

applyLiteDefaults();

if (prepareGameLayout()) {
  initializeRenderer();
  initializeMobileUi(styles);
}
