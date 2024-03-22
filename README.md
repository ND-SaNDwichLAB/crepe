

## File Structure



### Database design

We have a local android database using SQLite, and a firebase remote database

They are synced every hour, to ensure consistency

When uploading to firebase failed due to connectivity issues, we queue the upload operation until network connection gets established again. (Link to pieces of code)

When removing collectors, we do not really remove them from database to avoid data loss. Instead, we set their collectorStatus as “deleted” both locally and in firebase. All associated information (datafields and data) will not be queried if the associated collector’s status is deleted, or expired. [View code in FirebaseCommunicationManager.java](https://github.com/yuwen-lu/crepe/blob/5027e372de9bafab421245e5fc2e4b769c5105e0/app/src/main/java/edu/nd/crepe/network/FirebaseCommunicationManager.java#L89)

### Graph Query

Ranking algorithm and the use of LLM


## ui

In `CollectorConfigurationDialogWrapper.java`, we are maintaining a state called `currentScreenState`.


### Setup

If you are using Android Studio, you need to install `HAXM installer` to launch the project.

### Collector



### DataField



### UISnapShot

The innovation part of our app is programming by demonstration.

We pre-define multiple relationships between the elements. 

When user used the demonstration feature.



(conj (ABOVE (conj (hasText 11:26 AM) (HAS_CLASS_NAME android.widget.TextView) (HAS_PACKAGE_NAME com.google.android.deskclock)) ) (HAS_CLASS_NAME android.widget.TextView) (HAS_PACKAGE_NAME com.google.android.deskclock))

# ServiceManager

This folder mainly contains function that is related to the permission control and notification service. In order to use our app, we need to ask user to grant the permission of displaying information over other apps, accessibility control. These are the premise of using our app.

# Network

This folder contains the service that is mainly related to Firebase. In addition, we use third library, `OkHttp` and `Volley` to handle network requests and responses. These are the basic functions to handle creating, editing and deleting requests of the data related to collectors, users, data and dataFields.

# GraphQuery

GraphQuery is composed of 'Graph' and 'Query'.

In this folder, we create an abstract layer to expand details about current UI Elements. Based on the basic information, we define node, variable and block to form a graph that shows the relationships among these UI elements. This relationships could be represented in spatial, hierarchical and functional structures. In this graph, we also adds some new attributes of the UI Elements 

In addition, we define a set of criteria about how the query could work. This includes:
1. logical expression like `AND`, `OR`, etc.
2. Filtering logic like `ARG_MIN`, `ARG
MAX` and `EXISTS` etc.
3. Relation logic like `HAS_TEXT`, `IS_CLICKABLE` and `RIGHT_OF` etc.
4. Simplest Query with specific relations and entities.
5. Subqueries
6. PlaceholderQuery for the need to dynamically modify the structure of an existing query.

Based on the graph and query part, we could form a UISnapShot that captures a snapshot the current state of the UI Interface. The data structure contains a subject, predicate and object. For example, `Button X has text 'Submit'` or `Image Y is to the right of Text Z`. With this snapshot, we could analyze or interact with the UI programmatically. To be specific, we could use machine language to explain what users perceive or explain(This is the natural language)

# Demonstration

The demonstration is mainly to confirm the data that we want to collect and figure out the reason why the user want to collect this data.

In the demonstration process, the system could generate `GraphQuery` through users' `tap` interaction with the User Interface. When the user tap a text, the system will create a UISnapshot that shows all the attributes and possible relationships regarding the UI Element where the text stands for. After users write a description about their purpose, we ask generative AI to tell which relationships match the purpose. For example, the purpose of tapping the first element in the list(the element name is `Black Coffee`) and tapping the names that contains `Black coffee` is different. The first one could be interpreted as the fact that the user just want to select the top recommended coffee. But the second one could be interpreted as the fact that the user just want to drink black coffee.

# Database

In the database table, we have `Collector`, `Datafield`, `User` and `Data`. We also have a database manager feature to define several actions of manipulating the data.

# Authentication

This folder contains the activity of `Register`, `Login` and `Google Account authentication`.



