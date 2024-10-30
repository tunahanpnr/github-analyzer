# GitHub Java-ClassName Analyzer

## Installation

1. **Clone the repository:**

   ```bash
   git clone https://github.com/your-username/your-repo-name.git
   cd your-repo-name
   ```

2. **Install dependencies:**

   Ensure you have [Gradle](https://gradle.org/install/)
   and [JDK 17](https://docs.oracle.com/en/java/javase/23/install/overview-jdk-installation.html) or higher installed.

   To check if you have them installed, you can run:

   ```bash
   gradle -v
   java -version
   ```

   If both are installed, you should see version information displayed.

3. **Build the application:**

   Run the following command to download dependencies and compile the application:

   ```bash
   ./gradlew build
   ```

## Run the Application

Once the application has been built, you can run it using Gradle:

```bash
PORT="port number" GITHUB_TOKEN="github access token" ./gradlew run
```

This command starts the application. You should see output in the terminal indicating the application is running. Make
sure that you have changed the PORT and GITHUB_TOKEN parameters with yours.