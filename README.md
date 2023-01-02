# Set_Card_Game

Multithread version of the known game of "Set". This is an assignment part of SPL course in my Bachelor degree. I have implanted the "Dealer", "Table", and "Player" classes.  And game logics. 

# About

game Flow : The game contains a deck of 81 cards. Each card contains a drawing with four features (color,number, shape, shading). The game starts with 12 drawn cards from the deck that are placed on a 3x4 grid on the table. The goal of each player is to find a combination of three cards from the cards on the table that are said to make up a “legal set”. The game ends when there is no legal sets in the deck.

Supply the main thread that runs the table logics, the main thread that runs the "Dealer" that manages the game flow, and a single thread for each player.


# Main features 
1.	Use locks and atomic variables to manage the threads 
2.	Use synchronization concepts for a "Fair" game
3.	Fully support human players and computer players


# Screen Shots
| | |
|:-------------------------:|:-------------------------:|
|<img style="max-width:200px; width:100%"  src="https://user-images.githubusercontent.com/101994161/210243150-375851f9-1921-427d-8721-0e0f52953b42.png" alt="SreenShot1" >|<img style="max-width:200px; width:100%"  src="https://user-images.githubusercontent.com/101994161/210243154-e6b83830-a6ad-4c05-8855-7c7bc95fa58a.png" alt="SreenShot2" >



