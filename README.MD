
---

# CookBook 📱🍲

A feature-rich Android recipe app where users can discover, create, save, and manage recipes from various cuisines. This interactive app offers personalized profiles, following other foodies, saving your favorite meals, and more—all powered by Firebase.

## Table of Contents
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Setup](#setup)
- [Usage](#usage)
- [Contributing](#contributing)

## Features
- **User Registration & Login**: Secure sign-in using Firebase Authentication.
- **Add/Edit/Delete Recipes**: Create detailed recipes with images, categories, ingredients, and procedures.
- **Save Recipes**: Bookmark your favorite recipes to access them easily later.
- **Follow/Unfollow Users**: Stay connected with your favorite chefs and explore their creations.
- Recipe Feed Sections:
    - Featured Recipes
    - New Recipes
    - Quick & Easy
    - Veg & Non-Veg
- **Search Recipes**: Fast, responsive search with optional debounce for better performance.
- **Filter & Sort Recipes**: Sort by popularity, cook time, category, and more.
- **Dynamic Ingredients & Procedures**: Add unlimited steps and ingredients.
- **Responsive UI**: Modern and intuitive interface built with ConstraintLayout.

## Tech Stack
- **Frontend**: Kotlin, XML Layouts, ConstraintLayout
- **Backend**: Firebase Realtime Database, Firebase Authentication
- **Libraries**: 
    - Glide (image loading)
    - Firebase SDK
    - Material Components
    - RecyclerView with multiple view types
    - ConstraintLayout for modern UI
    - ViewBinding

## Setup
1. **Clone the Repository**:
    ```bash
    git clone https://github.com/bhanuprakashh1/Cook-Book.git
    cd Cook-Book
    ```

2. **Open in Android Studio**:
   - Open Android Studio and select "Open an existing project".
   - Navigate to the cloned project directory.

3. **Configure Firebase**:
   - Follow [Firebase's documentation](https://firebase.google.com/docs/android/setup) to set up Firebase for your app.
   - Download the `google-services.json` file and place it in the `app` directory.

4. **Build and Run**:
   - Sync the project with Gradle files.
   - Run the app on an Android emulator or physical device.

## Usage
- **Sign Up/Login**: Register with your name, email, and password.
- **Create Recipes**: Add rich content including image, ingredients, steps, and category.
- **Edit/Delete**: Modify or remove your recipes at any time.
- **Save Recipes**: Tap the save icon to bookmark a recipe.
- **Follow Chefs**: Click profiles to follow and stay updated with their recipes.
- **Explore Recipes**: Use the search or filter options to browse by cuisine, type, and more.
- **User Profile**: View your saved recipes, followers, following, and edit your bio or profession.

## Contributing
Contributions are welcome! Please open an issue or submit a pull request for any improvements.

1. Fork the project.
2. Create your feature branch (`git checkout -b feature/NewFeature`).
3. Commit your changes (`git commit -m 'Add new feature'`).
4. Push to the branch (`git push origin feature/NewFeature`).
5. Open a pull request.


---
