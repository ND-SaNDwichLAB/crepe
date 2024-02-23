

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

### Problem

No usage of `onDataLoadingEvent()` in `ui/main_activity/HomeFragment.java`