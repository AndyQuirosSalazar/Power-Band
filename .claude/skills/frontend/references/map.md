# Frontend

After you change this layer, update this file so the next run stays current.

Source: `src/app/ui`

## Implemented
- Swing main menu: `src/app/ui/MainMenu.java`

## Entrypoints
- `src/app/powerBand.java`

## Classes
### MainMenu
Displays the menu title and vertically arranged buttons. `src/app/ui/MainMenu.java`
- `MainMenu(Runnable pExitAction)` — builds the menu and runs the supplied action when `Salir` is clicked.

### powerBand
Application entrypoint that displays the Swing window. `src/app/powerBand.java`
- `main(String[] pArgs)` — creates and shows the main menu on the Swing Event Dispatch Thread.

### IConstants
Menu title, button labels, and title color. `src/app/util/IConstants.java`
