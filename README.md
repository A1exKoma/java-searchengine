# java-searchengine
The final project of the course "Java developer"

# How to use

**Project setup**
- Open the project in IntelliJ IDEA
- Add libraries from the "libs":
  - Go to: File -> Project Structure -> Project Settings - Libraries -> New Project Library (Sign "+") -> Java
  - Select all files in the "libs"
  - Press "OK" -> "Apply"
- To run the project you need MySQL
  - Create new Schema with name `search_engine`
  - Change fields "username" and "password" with your data in application.yaml: \src\main\resources\application.yaml
- In application.yaml you can also choose sites for indexing

**Project launch**
- Run "Application.java": \src\main\java\searchengine\Application.java
- In the browser, go to page: localhost:8080

**Site navigation**
- Dashboard:
  - Total statistics and detailed statistics on each site
  - To update information, you need update the page
- Management:
  - Starting and stopping indexing and Indexing of a single page
  - To Index a single page you need to enter the full path of the page in field and press "Add/Update"
- Search:
  - Search pages by quary:
    - Enter the quary in field
    - Select the site from the list or select "All sites"
    - Press "Search"
