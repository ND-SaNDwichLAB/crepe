
### Database design

We have a local android database using SQLite, and a firebase remote database

They are synced every hour, to ensure consistency

When uploading to firebase failed due to connectivity issues, we queue the upload operation until network connection gets established again. (Link to pieces of code)

When removing collectors, we do not really remove them from database to avoid data loss. Instead, we set their collectorStatus as “deleted” both locally and in firebase. All associated information (datafields and data) will not be queried if the associated collector’s status is deleted, or expired.

### Graph Query

Ranking algorithm and the use of LLM
