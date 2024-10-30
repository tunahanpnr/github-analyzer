# GitHub Java-ClassName Analyzer

## Installation and Running

### Recommended Way

1. **Clone the repository:**
      ```bash
   git clone https://github.com/your-username/your-repo-name.git
   cd your-repo-name
   ```
2. **Install docker compose into your system. [installation guide](https://docs.docker.com/compose/install/)**

3. **Run the containers**
   ```bash
   docker compose up
   ```
### Manual Installing
1. **Clone the repository as above**
2. **Install dependencies:**

   - [Gradle](https://gradle.org/install/)
   - [JDK 17](https://docs.oracle.com/en/java/javase/23/install/overview-jdk-installation.html) or higher installed.
   - [ElasticSearch](https://www.elastic.co/guide/en/elasticsearch/reference/current/install-elasticsearch.html)
   - [Kibana](https://www.elastic.co/guide/en/kibana/current/install.html)

3. Run all the services
   - Check the documentations for running ElasticSearch and Kibana
   - Then, just run the gradle as below.
   ```shell
   export $(cat .env | xargs) && ./gradlew run
   ```

3. **Build the application:**

   Run the following command to download dependencies and compile the application:

   ```bash
   ./gradlew build
   ```

## Important Notes for the App

### Loading GitHub Data
- The application currently has two distinct endpoints for saving data:
   - **"/save-repositories"**
      - This endpoint retrieves file contents and extracts all class names from them.
      - It is not advisable to use this option, as it generates numerous requests to the GitHub API, which may lead to connection errors after some time.

   - **"/save-repositories-light"**
      - This is a lighter alternative to the previous call.
      - It only retrieves file names without accessing their contents.
      - The method checks the URL suffix to derive class names, assuming that each file contains a public class matching its filename.
      - As a result, this method may miss actual class names if a file does not contain a public class, as the class name could differ from the filename.
      - Additionally, this approach does not capture all class names since it does not analyze the actual file content.

- Please keep these two scenarios in mind while using the application.

### Analyzing the Loaded Data
- For data analysis, we recommend using **ElasticSearch** and **Kibana**.
- **ElasticSearch** provides fast, accurate search capabilities while storing data in a NoSQL format.
- **Kibana** offers excellent tools for visualizing and analyzing the indexed data from ElasticSearch.

   - **Two key features include:**
      - **Discover Page**
         - The Discover page allows you to run queries against ElasticSearch and gain insights from the returned results.
         - To access this feature, open the sidebar and click on "Discover." This will load the first ten results, which you can adjust as needed.
         - You can then click on the "Field statistics" option to view examples and the top ten values from your query results.
         - The `repoFiles.classNames.keyword` tab provides details on class names.

      - **Dashboard**
         - The Dashboard visualizes a broader range of data.
         - It is recommended for obtaining a comprehensive overview of the dataset.
         - After configuring the dashboard, you can select fields from the left sidebar.
         - The data will then be displayed in various graph formats, presenting different types of information.

