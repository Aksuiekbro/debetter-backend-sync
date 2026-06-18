## Work in progress
# Debetter | A platform for hosting school and university debate tournaments

## Overview

The project was built using Spring Boot (and other Spring technologies), PostgreSQL, Redis, Lucene, Liquibase, Hibernate and others.

Users can register and login. There are two types of accounts, debaters and organizers. Organizers can create and manage tournaments, while debaters can join these tournaments by finding them in the search page.

## Tournament management

The main organizer can invite other organizers to manage their tournaments (done by entering the username of the other organizer). When creating the tournament, the organizer is asked to enter its name, round numbers, debate format and other information. Also, the organizer can later change tournament information in the dashboard. A thumbnail can be added for the tournament.

The tournament management dashboard allows the users with proper rights to add schedules, judges, announcements and edit rounds and teams. The judges and participants can be checked in by sending corresponding API requests. Announcements can be edited and deleted as well as schedules, judges and teams. The tournament can be started when all the teams are checked in and minimum team number requirement is met. Also, all the judges must be checked in too.

After starting the tournament, the organizer can generate matchups. Then, the results of matches can be sent using the appropriate request. After all the matches of a round are finished, the matchups for the next round can be generated. This generation prioritizes matching the teams from different clubs and who have not been matched up with each other before.

## Participant activities

Debaters can register to tournaments before the registration deadline. They are asked to enter their team name and club name. They can invite other debaters right away, or invite them later. They cannot check in until there are enough team members (differs for different formats). Currently, the system can handle APF (team vs team, 2 vs 2), BPF(team vs team, 3 vs 3) and LD (individual, 1 vs 1) formats. Additionally, the participants can change the team or club name.

While in tournaments, the participants can add comments to announcements and feedback to tournaments themselves.

## Database structure

Entity relationship diagram of all tables

![ERD](/entity-relationship-diagram.png)