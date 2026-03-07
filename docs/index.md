## LMS User Service

This project is a user service application for a Learning Management System (LMS), built using Spring Boot and Gradle.

### Project Details

1. Project Name: lms-user-service
2. Spring Boot Version: 3.3.3
3. Build Tool: Gradle
4. JDK Version: 21

### Getting Started

### Prerequisites

- Java Development Kit (JDK) 21
- Spring Boot 3.3.3
- Gradle 8.8 or higher
- Checkstyle 10.12.4
- Spotbugs 5.0.14
- NoSQL Workbench version 3.13.2
- SAM CLI version 1.90

### Configure DynamoDBLocal with NoSQL Workbench

1. Install `NoSQL Workbench version 3.13.2` and launch `DynamoDB`
2. Start the DynamoDB server by clicking the DDB Local toggle which starts the server in `http://localhost:8000`

3. Create a `DynamoDB data model` ex., UserModel
4. Now go to `Data modeler` and inside your model, create `user, roles, tenant` tables with their
   respective `partition key` and `sort key` and `additional attributes` to your tables
5. Now to `Operation Builder` and click `New Connection` and create a `local dynamodb connection` with a unique name
6. Then click on `Visualizer` to visualize your table structure and then click on `Commit to Amazon DynamoDB` button and
   insert tables created to your local connection
7. Now inside your `local connection` you see your tables created
8. For the `user` table you need to create `GSIs(Global Secondary Index)` for `sorting` purpose
9. Click on the `three dots` besides your user table to `create GSI`

#### Required GSIs

| globalIndexName     | partitionKey | sortKey               |
|:--------------------|:-------------|:----------------------|
| gsi_sort_createdOn  | tenantCode	  | createdOn             |
| gsi_sort_name	      | tenantCode	  | gsiSortFNLN           |
| gsi_sort_status	    | tenantCode	  | status                |
| gsi_sort_userType	  | tenantCode	  | userType              |
| gsi_sort_expiryDate | tenantCode	  | userAccountExpiryDate |

### Configure Environment Variables

1. Configure environment variables in your `Itellij IDE`
2. Click on the `Add New Configuration` on top center of IDE
3. Now create `gradle configuration` with name `lms-user-service[bootRun]`
4. Give the run command as `bootRun`
5. In the `environment variables` section add the required environment variables

#### Required ENV Variables

```bash 
APP_ENV=local;
AWS_COGNITO_USER_POOL_ID=<user-pool-id>;
AWS_DYNAMODB_ENDPOINT=<dynamodb-local-endpoint>;
AWS_DYNAMODB_ROLES_TABLE_NAME=role;
AWS_DYNAMODB_ROLES_TABLE_PARTITION_KEY_NAME=pk;
AWS_DYNAMODB_USER_TABLE_DEFAULT_SORT_KEY=createdOn;
AWS_DYNAMODB_USER_TABLE_DEFAULT_SORT_ORDER=desc;
AWS_DYNAMODB_USER_TABLE_NAME=user;
AWS_DYNAMODB_USER_TABLE_PARTITION_KEY_NAME=tenantCode;
REGION_NAME=us-east-1;
AWS_S3_BUCKET_NAME=<s3-bucket>;
LOCAL_STORAGE_PATH=<your-local-path>;
DEFAULT_ROWS_PER_PAGE=10
```

### Build Commands

Build your application:

```bash
./gradlew assemble
```

assemble will run the build and any tasks that have been linked.

Run your project:

```bash
./gradlew bootRun
```

bootRun will result in gradle launching your project.

To run code quality checks using Spotbug and Checkstyle gradle plugin:

```bash
./gradlew checkstyleMain
./gradlew spotbugsMain
./gradlew spotbugsTest
```

Report output can be seen at:

```bash
build/reports/checkstyle/main.html
build/reports/checkstyle/test.html

build/reports/spotbugs/main.html
build/reports/spotbugs/test.html

```

### Alternate Methods For Launching

* From your IDE, look for src/main/java/com/cognizant/lms/userservice/LmsUserServiceApplication.java.
* You should be able to Right-Click to Run or Debug the application.
    * The keyboard shorcuts can help here.
        * Run: &lt;CTRL&gt;&lt;SHIFT&gt; R
        * Debug: &lt;CTRL&gt;&lt;SHIFT&gt; D

### Running Application As Lambda Function

1. Install `SAM CLI version 1.90`
2. Create a file called `env.json` which consists of environment for local run of your application
3. Example of env.json
```json
{
  "LmsUserServiceContainerFunction": {
    "APP_ENV": "local",
    "AWS_COGNITO_USER_POOL_ID": "<user-pool-id>",
    "AWS_DYNAMODB_ENDPOINT": "http://host.docker.internal:8000/",
    "AWS_DYNAMODB_ROLES_TABLE_NAME": "role",
    "AWS_DYNAMODB_ROLES_TABLE_PARTITION_KEY_NAME": "pk",
    "AWS_DYNAMODB_USER_TABLE_DEFAULT_SORT_KEY": "createdOn",
    "AWS_DYNAMODB_USER_TABLE_DEFAULT_SORT_ORDER": "desc",
    "AWS_DYNAMODB_USER_TABLE_NAME": "user",
    "AWS_DYNAMODB_USER_TABLE_PARTITION_KEY_NAME": "tenantCode",
    "REGION_NAME": "us-east-1",
    "AWS_S3_BUCKET_NAME": "<s3-bucket>",
    "LOCAL_STORAGE_PATH": "<your-local-path>",
    "DEFAULT_ROWS_PER_PAGE": "10"
  }
}
```
4. Run the following command to build the project
```bash
sam build
```
5. Run the following command to start the application as `Lambda Function with container image support`
```bash
sam local start-api --docker-network host --env-vars env.json
```






