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

