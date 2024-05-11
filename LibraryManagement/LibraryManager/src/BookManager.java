import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

public class BookManager {
    //Creatiion of the File Variables Names
    private static final String FILENAME = "books.txt";
    private static final String LOGIN_FILE = "login.txt";
    private static final String BOOK_REQUESTS_FILE = "bookrequests.txt";
    private static final double DAILY_FINE_RATE = 1.5;
    private static final Scanner scanner = new Scanner(System.in);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    //Creation of Main Menu Of Program
    public static void main(String[] args) throws InterruptedException {
        // Schedule the notifications task to run
        scheduler.scheduleAtFixedRate(new NotificationsTask(), 0, 20, TimeUnit.MINUTES);

        TimeUnit.MILLISECONDS.sleep(50);

        //Main MENU
        mainMenu();
    }    

    //MARK: NOTIFICATIONS
    static class NotificationsTask implements Runnable {
        @Override
        public void run() {
            // Implement notification logic here
            System.out.println("\nSending notifications to members with due or overdue books...");
        }
    }

    private static void viewNotificationsForDueBooks(String loggedInUsername) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("loans.txt"))) {
            boolean hasNotifications = false;
            String line;
            System.out.println();
            System.out.println("Notifications for due books and any Fines:");
            System.out.println();
            LocalDate today = LocalDate.now();

            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 6 && parts[0].equals(loggedInUsername)) {
                    String bookName = parts[1];
                    LocalDate dueDate = LocalDate.parse(parts[5]);
                    boolean isDue = dueDate.isBefore(today);
                    long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);
                    boolean isDueIn7Days = !isDue && daysUntilDue <= 14 && daysUntilDue >= 0;

                    // Calculate fine
                    double fine = calculateFine(dueDate);

                    // Display notifications for books due or due in 7 days
                    if (isDue || isDueIn7Days) {
                        hasNotifications = true;
                        System.out.println("Book: " + bookName);
                        System.out.println("Due: " + (isDue ? "Yes" : "No"));
                        System.out.println("Due Date: " + parts[5]); // Display due date
                        System.out.println("Fine: $" + fine);
                        if (isDueIn7Days) {
                            System.out.println("Due in " + daysUntilDue + " days");
                        }
                        System.out.println();
                    }
                }
            }

            if (!hasNotifications) {
                System.out.println("No notifications.");
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //MARK: MAIN MENU
    //Main MENU
    private static void mainMenu() {
        while (true) {
            try {
                System.out.println("Main Menu:");
                System.out.println("1. Login");
                System.out.println("2. Create Account");
                System.out.println("3. Exit");
                System.out.print("Enter your choice: ");
                int choice = scanner.nextInt();
                scanner.nextLine();
    
                switch (choice) {
                    case 1:
                        login();
                        break;
                    case 2:
                        createAccount();
                        break;
                    case 3:
                        System.out.println("Exiting...");
                        scanner.close(); // Close scanner before exiting
                        scheduler.shutdown(); // Shutdown the scheduler
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter a valid option.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine(); // clear the input buffer
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
            }
        }
    }

    //The Login Menu System
     // MARK: Login
     private static void login() {
        System.out.print("Enter username or email: ");
        String input = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
    
        if (authenticate(input, password)) {
            System.out.println("Login successful.");
            if (isAdmin(input)) {
                adminMenu();
            } else {
                userMenu(input); // Pass the username to the userMenu method
            }
        } else {
            System.out.println("Invalid username, email, or password.");
        }
    }    

    //To Check If User Is ADMIN
     // MARK: Admin
    private static boolean isAdmin(String input) {
        return input.equals("ADMIN") || input.equalsIgnoreCase("ADMIN@gmail.com");
    }    

    //To Check If User Is ADMIN
    private static boolean authenticate(String input, String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader(LOGIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2 && (parts[0].equals(input) || parts[2].equals(input)) && parts[1].equals(password)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading user data: " + e.getMessage());
        }
        return false;
    }

    //Creation Of A User Acount
     // MARK: Create Accounts
    private static void createAccount() {
        System.out.print("Enter new username: ");
        String username = scanner.nextLine();
        System.out.print("Enter new password: ");
        String password = scanner.nextLine();
        System.out.print("Enter your email: ");
        String email = scanner.nextLine();
    
        if (!email.toLowerCase().endsWith("@gmail.com")) {
            System.out.println("Invalid email address. Please enter a Gmail address.");
            return;
        }

        // Check if username already exists
        try (BufferedReader reader = new BufferedReader(new FileReader(LOGIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 0 && parts[0].equals(username)) {
                    System.out.println("Username already exists. Please choose a different username.");
                    return;
                }
            }
        } catch (IOException e) {
            System.out.println("Error checking username availability: " + e.getMessage());
            return;
        }
    
        // If username is unique, proceed with account creation
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOGIN_FILE, true))) {
            writer.write(username + "," + password + "," + email);
            writer.newLine();
            System.out.println("Account created successfully.");
            System.out.println("Returning to main menu...");
        } catch (IOException e) {
            System.out.println("Error creating account: " + e.getMessage());
        }
    }

    //The User Menu System
     // MARK: User Menu
     private static void userMenu(String loggedInUsername) {
        while (true) {
            try {
                System.out.println("User Menu:");
                System.out.println("1. View Books");
                System.out.println("2. Request New Books Not In Our Library");
                System.out.println("3. View Checked Out Books and Fines");
                System.out.println("4. View Notifications for Due Books and Fines");
                System.out.println("5. Exit");
                System.out.print("Enter your choice: ");
                int choice = scanner.nextInt();
                scanner.nextLine(); // consume newline
    
                switch (choice) {
                    case 1:
                        viewAllBooks();
                        break;
                    case 2:
                        requestBook();
                        break;
                    case 3:
                        viewCheckedOutBooksAndFines(loggedInUsername);
                        break;
                    case 4:
                        viewNotificationsForDueBooks(loggedInUsername);
                        break;
                    case 5:
                        System.out.println("Returning to main menu...");
                        return;
                    default:
                        System.out.println("Invalid choice. Please enter a valid option.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine();
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
            }
        }
    }
    
    //Allows Users to Request A Book If Not in The Library Currently
     // MARK: Request Books Ueer
    private static void requestBook() {
        try {
            FileWriter fileWriter = new FileWriter(BOOK_REQUESTS_FILE, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
    
            System.out.print("Enter book name: ");
            String bookName = scanner.nextLine().trim();
            System.out.print("Enter author name: ");
            String author = scanner.nextLine().trim();
            System.out.print("Enter publication date: ");
            String date = scanner.nextLine().trim();
            System.out.print("Enter ISBN: ");
            String isbn = scanner.nextLine().trim();
    
            // Get username from login
            System.out.print("Enter your username: ");
            String username = scanner.nextLine().trim();
    
            String line = bookName + "," + author + "," + date + "," + isbn + "," + username;
            bufferedWriter.write(line);
            bufferedWriter.newLine();
            bufferedWriter.close();
    
            System.out.println("Book request sent successfully!");
            System.out.println("We will try our best to get this book into stock!");
    
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }
    
    // Admin Menu System
     // MARK: Admin Menu
    private static void adminMenu() {
        while (true) {
            try {
                System.out.println("Admin Menu:");
                System.out.println("1. Add a new book");
                System.out.println("2. View all books");
                System.out.println("3. Delete a book");
                System.out.println("4. View requested books");
                System.out.println("5. Checkout Books");
                System.out.println("6. View checked out books");
                System.out.println("7. Exit");
                System.out.print("Enter your choice: ");
                int choice = scanner.nextInt();
                scanner.nextLine(); // consume newline

                switch (choice) {
                    case 1:
                        addNewBook();
                        break;
                    case 2:
                        viewAllBooks();
                        break;
                    case 3:
                        deleteBook();
                        break;
                    case 4:
                        viewRequestedBooks();
                        break;
                    case 5:
                        checkoutBook();
                        break;
                    case 6:
                        viewCheckedOutBooks(true);
                        break;
                    case 7:
                        System.out.println("Exiting...");
                        return;
                    default:
                        System.out.println("Invalid choice. Please enter a valid option.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine();
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
            }
        }
    }

    private static boolean isUserValid(String username) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("login.txt"))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 1 && parts[0].trim().equalsIgnoreCase(username)) {
                    return true; // Username exists
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        return false; // Username does not exist
    } 
    
    //Checkout Books
     // MARK: Checkout
     private static void checkoutBook() {
        try {
            // Display available books
            System.out.println("Available Books:");
            viewAllBooks();
            
            System.out.print("Enter the number of the book you want to check out: ");
            int checkoutChoice = scanner.nextInt();
            scanner.nextLine();
    
            // Get the selected book
            String selectedBook = getBookByNumber(checkoutChoice);
            if (selectedBook != null) {
                // Check if the book is already checked out
                if (isBookCheckedOut(selectedBook)) {
                    System.out.println("Sorry, this book is already checked out. Please select a different book.");
                    return;
                }
                
                // Verify username of the user
                while (true) {
                    System.out.print("Enter the username of the user (Enter 0 to cancel): ");
                    String username = scanner.nextLine().trim();
                    
                    if (username.equals("0")) {
                        System.out.println("Checkout canceled.");
                        return;
                    }
                    
                    if (!isUserValid(username)) {
                        System.out.println("Invalid username. Please enter a valid username or enter 0 to cancel.");
                    } else {
                        // Assuming a simple text file tracking checked out books
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter("checked_out_books.txt", true))) {
                            writer.write(selectedBook + "," + username); // Save the book and username
                            writer.newLine();
                            System.out.println("Book checked out successfully!");
    
                            // Add loan to user's account
                            addLoan(username, selectedBook);
    
                            // Calculate overdue fine
                            calculateAndAddFine(username);
    
                            break;
                        } catch (IOException e) {
                            System.out.println("Error checking out book: " + e.getMessage());
                        }
                    }
                }
            } else {
                System.out.println("Invalid book choice.");
            }
        } catch (InputMismatchException e) {
            System.out.println("Invalid input. Please enter a number.");
            scanner.nextLine(); // Clear the input buffer
        }
    }   

    // Add loan to user's account
    private static void addLoan(String username, String bookDetails) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("loans.txt", true))) {
            LocalDate dateTaken = LocalDate.now();
            // Assuming bookDetails includes title, author, and due date
            writer.write(username + "," + bookDetails + "," + dateTaken);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Error adding loan: " + e.getMessage());
        }
    }    

    // Calculate and add overdue fine to user's account
    private static void calculateAndAddFine(String username) {
        try (BufferedReader reader = new BufferedReader(new FileReader("loans.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2 && parts[0].equals(username)) {
                    // Assuming parts[1] includes book details with due date
                    String[] bookDetails = parts[1].split("-");
                    if (bookDetails.length >= 2) {
                        LocalDate dueDate = LocalDate.parse(bookDetails[1].trim()); // Parse due date
                        double fine = calculateOverdueFine(dueDate);
                        if (fine > 0) {
                            // Add fine to user's account
                            addFine(username, fine);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading loan data: " + e.getMessage());
        }
    }

    // Calculate overdue fine
    private static double calculateOverdueFine(LocalDate dueDate) {
        LocalDate currentDate = LocalDate.now();
        long daysOverdue = currentDate.compareTo(dueDate); // Calculate days overdue
        if (daysOverdue > 0) {
            return daysOverdue * DAILY_FINE_RATE;
        }
        return 0;
    }

    // Add fine to user's account
    private static void addFine(String username, double fineAmount) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("fines.txt", true))) {
            writer.write(username + "," + fineAmount);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Error adding fine: " + e.getMessage());
        }
    }
    
    private static boolean isBookCheckedOut(String selectedBook) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("checked_out_books.txt"))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith(selectedBook)) {
                    return true; // Book is checked out
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        return false; // Book is not checked out
    }      

    // Implement the getBookByNumber() method:
    private static String getBookByNumber(int number) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(FILENAME))) {
            String line;
            int count = 0; // Start counting from 1
            while ((line = bufferedReader.readLine()) != null) {
                if (count == number) {
                    // Check if the book is available (not checked out)
                    // You can add additional logic here based on your book format
                    return line;
                }
                count++;
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        return null;
    }

     // MARK: View Checkouts
     private static void viewCheckedOutBooks(boolean isAdminMenu) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("loans.txt"))) {
            System.out.println("Checked Out Books:");
            System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------");
            System.out.printf("%-30s%-25s%-20s%-25s%-20s%-20s%-10s\n", "Title", "Author", "Username", "ISBN", "Date Taken Out", "Due Date", "Fine");
            System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------");
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 6) {
                    String username = parts[0].trim();
                    String bookName = parts[1].trim();
                    String author = parts[2].trim();
                    String isbn = parts[4].trim();
                    String dateTaken = parts[5].trim();
    
                    // Parse date taken and calculate due date
                    LocalDate dateTakenOut = LocalDate.parse(dateTaken);
                    LocalDate dueDate = dateTakenOut.plusDays(14);
    
                    // Calculate fine
                    double fine = calculateFine(dateTakenOut);
    
                    // Display the book details with fine
                    System.out.printf("%-30s%-25s%-20s%-25s%-20s%-20s", bookName, author, username, isbn, dateTakenOut, dueDate);
                    if (fine > 0) {
                        System.out.printf("R%.2f\n", fine);
                    } else {
                        System.out.println("No fine");
                    }
                }
            }
            System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------");
            if (isAdminMenu) {
                System.out.println("Select an option:");
                System.out.println("1. Remove a book from checked out list");
                System.out.println("2. Return to Admin Menu");
                System.out.print("Enter your choice: ");
                int option = scanner.nextInt();
                scanner.nextLine(); // consume newline
    
                switch (option) {
                    case 1:
                        removeCheckedOutBook();
                        break;
                    case 2:
                        System.out.println("Returning to Admin Menu...");
                        break;
                    default:
                        System.out.println("Invalid option.");
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
        }
    }            
    
     // MARK: Remove Checkouts
     private static void removeCheckedOutBook() {
        try {
            System.out.print("Enter the number of the book you want to return: ");
            int returnChoice = scanner.nextInt();
            scanner.nextLine(); // consume newline
    
            List<String> loans = new ArrayList<>();
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader("loans.txt"))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    loans.add(line);
                }
            }
    
            if (returnChoice >= 1 && returnChoice <= loans.size()) {
                String removedLoan = loans.remove(returnChoice - 1); // Remove the selected loan
    
                // Update the loans.txt file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("loans.txt"))) {
                    for (String loan : loans) {
                        writer.write(loan);
                        writer.newLine();
                    }
                }
    
                // Extract the book details from the removed loan
                String[] parts = removedLoan.split(",");
                if (parts.length == 6) {
                    String bookDetails = parts[1] + "," + parts[2] + "," + parts[3] + "," + parts[4]; // Assuming book details include title, author, ISBN, and date taken
                    removeFromCheckedOutBooks(bookDetails); // Remove the book from checked_out_books.txt
                }
    
                System.out.println("Book returned successfully!");
            } else {
                System.out.println("Invalid choice.");
            }
        } catch (IOException e) {
            System.out.println("Error returning book: " + e.getMessage());
        }
    }
    
    private static void removeFromCheckedOutBooks(String bookDetails) {
        try {
            List<String> checkedOutBooks = new ArrayList<>();
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader("checked_out_books.txt"))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (!line.startsWith(bookDetails)) {
                        checkedOutBooks.add(line);
                    }
                }
            }
    
            // Update the checked_out_books.txt file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("checked_out_books.txt"))) {
                for (String book : checkedOutBooks) {
                    writer.write(book);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.out.println("Error removing book from checked out list: " + e.getMessage());
        }
    }    

    // MARK: FINE STUFF
    private static double calculateFine(LocalDate dueDate) {
        LocalDate currentDate = LocalDate.now();
        LocalDate dueDatePlusGracePeriod = dueDate.plusDays(14); // Adding 14 days grace period
        if (currentDate.isBefore(dueDatePlusGracePeriod)) {
            return 0; // No fine during the grace period
        }
        long daysOverdue = ChronoUnit.DAYS.between(dueDatePlusGracePeriod, currentDate);
        if (daysOverdue > 0) {
            return daysOverdue * DAILY_FINE_RATE;
        }
        return 0;
    }           

    private static void viewCheckedOutBooksAndFines(String loggedInUsername) {
        boolean isAdmin = isAdmin(loggedInUsername); // Assuming you have a method to check if the user is an admin
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("loans.txt"))) {
            System.out.println("Checked Out Books and Fines:");
            System.out.println("-------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
            System.out.printf("%-50s%-30s%-20s%-15s%-20s%-20s%-10s\n", "Title", "Author", "ISBN", "Username", "Date Taken", "Due Date", "Fine");
            System.out.println("-------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
    
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 6) {
                    String title = parts[1];
                    String author = parts[2];
                    String isbn = parts[4];
                    String username = parts[0];
                    String dateTaken = parts[5];
                    LocalDate dueDate = LocalDate.parse(dateTaken); // Due date without the grace period
                    double fine = calculateFine(dueDate);
    
                    if (!isAdmin && !username.equals(loggedInUsername)) {
                        continue; // Skip books not belonging to the logged-in user if not an admin
                    }
    
                    System.out.printf("%-50s%-30s%-20s%-15s%-20s%-20s", title, author, isbn, username, dateTaken, dueDate);
                    if (fine > 0) {
                        System.out.printf("R%.2f\n", fine);
                    } else {
                        System.out.println("No fine");
                    }
                }
            }
    
            System.out.println("-------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
        }
    }    
    
    //Allows Admins To View The Requested Books By Users
     // MARK: Admin see Requested
    private static void viewRequestedBooks() {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(BOOK_REQUESTS_FILE))) {
            List<String> requestedBooks = new ArrayList<>();
            System.out.println("Requested Books:");
            System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------");
            System.out.printf("%-5s%-50s%-30s%-20s%-30s%-20s\n", "No.", "Title", "Author", "Date", "ISBN", "Username");
            System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------");
            String line;
            int count = 1;
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    requestedBooks.add(line);
                    System.out.printf("%-5d%-50s%-30s%-20s%-30s%-20s\n", count++, parts[0], parts[1], parts[2], parts[3], parts[4]);
                }
            }
            System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------");
            System.out.println("Select an option:");
            System.out.println("1. Add a requested book to books.txt");
            System.out.println("2. Delete a requested book");
            System.out.println("3. Return to Admin Menu");
            System.out.print("Enter your choice: ");
            int option = scanner.nextInt();
            scanner.nextLine();
    
            switch (option) {
                case 1:
                    System.out.print("Enter the number of the book you want to add: ");
                    int addChoice = scanner.nextInt();
                    scanner.nextLine();
                    if (addChoice >= 1 && addChoice <= requestedBooks.size()) {
                        String selectedBook = requestedBooks.get(addChoice - 1);
                        addRequestedBook(selectedBook);
                    } else {
                        System.out.println("Invalid choice.");
                    }
                    break;
                case 2:
                    System.out.print("Enter the number of the book you want to delete: ");
                    int deleteChoice = scanner.nextInt();
                    scanner.nextLine();
                    if (deleteChoice >= 1 && deleteChoice <= requestedBooks.size()) {
                        String selectedBook = requestedBooks.get(deleteChoice - 1);
                        deleteRequestedBook(selectedBook);
                    } else {
                        System.out.println("Invalid choice.");
                    }
                    break;
                case 3:
                    System.out.println("Returning to Admin Menu...");
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }    
    
    // Allows Admins To Add The requested Book To The Books View List If The Book Can be Added
     // MARK: Add Requested
    private static void addRequestedBook(String selectedBook) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILENAME, true))) {
            // Split the selected book into parts
            String[] parts = selectedBook.split(",");
            if (parts.length >= 5) { // Ensure the selected book has at least 5 parts
                // Write the book details without the username to the books.txt file
                writer.write(parts[0] + "," + parts[1] + "," + parts[2] + "," + parts[3]);
                writer.newLine();
                System.out.println("Book added successfully!");
                removeRequestedBook(selectedBook); // Remove the book from the request list
            } else {
                System.out.println("Invalid book format.");
            }
        } catch (IOException e) {
            System.out.println("Error adding book to books.txt: " + e.getMessage());
        }
    }   
    
    //Allows Admin To Delete USer Book Requests If Book Shoulddnt Be Added To lIbrary
    // MARK: Delete Requested
    private static void deleteRequestedBook(String selectedBook) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(BOOK_REQUESTS_FILE));
             BufferedWriter writer = new BufferedWriter(new FileWriter(BOOK_REQUESTS_FILE))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.equals(selectedBook)) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            System.out.println("Book request deleted successfully!");
        } catch (IOException e) {
            System.out.println("Error deleting book request: " + e.getMessage());
        }
    }    
    
    //Removes Book From Requested Books If it is Added To Library So there Arent Duplicates Accidentally Added
    // MARK: Remove Requested
    private static void removeRequestedBook(String selectedBook) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(BOOK_REQUESTS_FILE));
             BufferedWriter writer = new BufferedWriter(new FileWriter(BOOK_REQUESTS_FILE))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.equals(selectedBook)) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            System.out.println("Book request removed successfully!");
        } catch (IOException e) {
            System.out.println("Error removing book request: " + e.getMessage());
        }
    }    

    //Lets Admin Add New Book To The Library
    // MARK: Add New Book
    private static void addNewBook() {
        try {
            FileWriter fileWriter = new FileWriter(FILENAME, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            System.out.print("Enter book name: ");
            String bookName = scanner.nextLine().trim();
            System.out.print("Enter author name: ");
            String author = scanner.nextLine().trim();
            System.out.print("Enter publication date: ");
            String date = scanner.nextLine().trim();
            System.out.print("Enter ISBN: ");
            String isbn = scanner.nextLine().trim();

            // Enclose book name in double quotes if it contains commas
            if (bookName.contains(",")) {
                bookName = "\"" + bookName + "\"";
            }

            String line = bookName + "," + author + "," + date + "," + isbn;
            bufferedWriter.write(line);
            bufferedWriter.newLine();
            bufferedWriter.close();

            System.out.println("Book added successfully!");

        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }

    //Displays all books
     // MARK: All Books View
    private static void viewAllBooks() {
        try {
            File file = new File(FILENAME);
            if (!file.exists()) {
                file.createNewFile();
                System.out.println("No books found. Created a new file.");
                return;
            }
    
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
    
            @SuppressWarnings("unused")
            String line;
            if ((line = bufferedReader.readLine()) == null) {
                System.out.println("No books found.");
                bufferedReader.close();
                return;
            }
    
            System.out.println("Select an option:");
            System.out.println("1. Show all books");
            System.out.println("2. Show books by author");
            System.out.print("Enter your choice: ");
            int option = scanner.nextInt();
            scanner.nextLine();
    
            if (option == 1) {
                displayAllBooks(bufferedReader);
            } else if (option == 2) {
                displayBooksByAuthor(bufferedReader);
            } else {
                System.out.println("Invalid option.");
            }
    
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        } catch (InputMismatchException e) {
            System.out.println("Invalid input. Please enter a number.");
            scanner.nextLine();
        }
    }
    
    // Display all books excluding the checked-out ones
     // MARK: All Books Except Checkout
    private static void displayAllBooks(BufferedReader bufferedReader) throws IOException {
        System.out.println("List of Books:");
        int count = 1;
        System.out.println("--------------------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-5s%-50s%-30s%-20s%-15s%-10s\n", "No.", "Title", "Author", "Date", "ISBN", "");
        System.out.println("--------------------------------------------------------------------------------------------------------------------------------");
        
        String line = bufferedReader.readLine(); // Read the first line
        while (line != null) {
            // Check if the book is not checked out
            if (!isBookCheckedOut(line)) {
                String[] parts = splitByCommaIgnoringQuotes(line);
                if (parts.length >= 4) {
                    String bookName = parts[0].trim();
    
                    // Replace multiple spaces with a single space
                    bookName = bookName.replaceAll("\\s+", " ");
    
                    System.out.printf("%-5d%-50s%-30s%-20s%-15s%-10s\n", count++, bookName, parts[1], parts[2], parts[3], "");
                } else {
                    System.out.println("Error: Incomplete data in line: " + line);
                }
            }
            line = bufferedReader.readLine();
        }
        System.out.println("--------------------------------------------------------------------------------------------------------------------------------");
        bufferedReader.close();
    }    

    //Only Displays Books For A Certain Author Choosen
     // MARK: Books By Author
    private static void displayBooksByAuthor(BufferedReader bufferedReader) throws IOException {
        Set<String> authors = new HashSet<>();
    
        // Extract unique author names from the file
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] parts = splitByCommaIgnoringQuotes(line);
            if (parts.length >= 2) {
                authors.add(parts[1].trim());
            }
        }
        bufferedReader.close();
    
        // Display the list of authors
        if (authors.isEmpty()) {
            System.out.println("No authors found.");
        } else {
            System.out.println("List of Authors:");
            int count = 1;
            for (String author : authors) {
                System.out.println(count++ + ". " + author);
            }
    
            System.out.print("Enter the number of the author whose books you want to view: ");
            int authorChoice = scanner.nextInt();
            scanner.nextLine(); // consume newline
    
            if (authorChoice < 1 || authorChoice > authors.size()) {
                System.out.println("Invalid choice. Please enter a valid author number.");
                return;
            }
    
            // Get the selected author name
            String selectedAuthor = null;
            int index = 1;
            for (String author : authors) {
                if (index == authorChoice) {
                    selectedAuthor = author;
                    break;
                }
                index++;
            }
    
            // Display books by the selected author
            displayBooksBySelectedAuthor(selectedAuthor);
        }
    }

    //Only Displays Books For A Certain Author Choosen
    private static void displayBooksBySelectedAuthor(String authorName) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(FILENAME))) {
            System.out.println("Books by " + authorName + ":");
            int count = 1;
            System.out.println("--------------------------------------------------------------------------------------------------------------------------------");
            System.out.printf("%-5s%-50s%-30s%-20s%-15s%-10s\n", "No.", "Title", "Author", "Date", "ISBN", "");
            System.out.println("--------------------------------------------------------------------------------------------------------------------------------");
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = splitByCommaIgnoringQuotes(line);
                if (parts.length >= 2 && parts[1].trim().equalsIgnoreCase(authorName)) {
                    String bookName = parts[0].trim();
                    bookName = bookName.replaceAll("\\s+", " ");
                    System.out.printf("%-5d%-50s%-30s%-20s%-15s%-10s\n", count++, bookName, parts[1], parts[2], parts[3], "");
                }
            }
            System.out.println("--------------------------------------------------------------------------------------------------------------------------------");
        }
    }

    //Lets Admin Remove A Book Completely from Library
     // MARK: Remove Books
    private static void deleteBook() {
        try {
            FileReader fileReader = new FileReader(FILENAME);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            List<String> lines = new ArrayList<>();
    
            // Skip the initial display of books
            bufferedReader.readLine(); // Skip the header row
    
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
            bufferedReader.close();
    
            // Display the header row
            System.out.println("--------------------------------------------------------------------------------------------------------------------------------");
            System.out.printf("%-5s%-50s%-30s%-20s%-15s%-10s\n", "No.", "Title", "Author", "Date", "ISBN", "");
            System.out.println("--------------------------------------------------------------------------------------------------------------------------------");
    
            // Display the books
            int count = 1;
            for (String entry : lines) {
                String[] parts = splitByCommaIgnoringQuotes(entry);
                String bookName = parts[0].trim();
                // Replace multiple spaces with a single space
                bookName = bookName.replaceAll("\\s+", " ");
                System.out.printf("%-5d%-50s%-30s%-20s%-15s%-10s\n", count++, bookName, parts[1], parts[2], parts[3], "");
            }
            System.out.println("--------------------------------------------------------------------------------------------------------------------------------");
    
            // Prompt the user to enter the number of the book they want to delete
            System.out.print("Enter the number of the book you want to delete: ");
            int deleteChoice = scanner.nextInt();
            scanner.nextLine(); // consume newline
    
            if (deleteChoice < 1 || deleteChoice > lines.size()) {
                System.out.println("Invalid choice. Please enter a valid book number.");
                return;
            }
    
            lines.remove(deleteChoice - 1);
    
            // Rewrite the updated list of books to the file
            FileWriter fileWriter = new FileWriter(FILENAME);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            for (String updatedEntry : lines) {
                bufferedWriter.write(updatedEntry);
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
    
            System.out.println("Book deleted successfully!");
    
        } catch (IOException e) {
            System.out.println("Error deleting book: " + e.getMessage());
        }
    }     

    //Split a string by comma, ignoring commas inside double quotes
    private static String[] splitByCommaIgnoringQuotes(String input) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }

        result.add(sb.toString());

        return result.toArray(new String[0]);
    }
}
