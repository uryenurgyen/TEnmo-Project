package com.techelevator.tenmo;

import com.techelevator.tenmo.model.*;
import com.techelevator.tenmo.service.*;
import com.techelevator.tenmo.dto.TransferDto;

import java.math.BigDecimal;
import java.util.List;

public class App {

    // Base URL for API endpoints
    private static final String API_BASE_URL = "http://localhost:8080/";

    // Service objects for console interaction, authentication, and transfers
    private final ConsoleService consoleService = new ConsoleService();
    private final AuthenticationSvcs authenticationService = new AuthenticationSvcsImpl(API_BASE_URL);
    private final TransferSvcs transferService = new TransferSvcsImpl(API_BASE_URL);

    // Stores the currently authenticated user
    private AuthenticatedUser currentUser;

    // Main method to run the application
    public void run() {
        consoleService.printGreeting();
        loginMenu();
        if (currentUser != null) {
            mainMenu();
        }
    }

    // Handles the login/register menu
    private void loginMenu() {
        int menuSelection = -1;
        while (menuSelection != 0 && currentUser == null) {
            consoleService.printLoginMenu();
            menuSelection = consoleService.promptForMenuSelection("Please choose an option: ");
            if (menuSelection == 1) {
                handleRegister();
            } else if (menuSelection == 2) {
                handleLogin();
            } else if (menuSelection != 0) {
                System.out.println("Invalid Selection");
                consoleService.pause();
            }
        }
    }

    // Handles user registration
    private void handleRegister() {
        System.out.println("Please register a new user account");
        UserCredentials credentials = consoleService.promptForCredentials();
        if (authenticationService.register(credentials)) {
            System.out.println("Registration successful. You can now login.");
        } else {
            consoleService.printErrorMessage();
        }
    }

    // Handles user login
    private void handleLogin() {
        UserCredentials credentials = consoleService.promptForCredentials();
        currentUser = authenticationService.login(credentials);
        if (currentUser == null) {
            consoleService.printErrorMessage();
        } else {
            String token = currentUser.getToken();
            transferService.setAuthToken(token);
        }
    }

    // Displays and handles the main menu options
    private void mainMenu() {
        int menuSelection = -1;
        while (menuSelection != 0) {
            consoleService.printMainMenu();
            menuSelection = consoleService.promptForMenuSelection("Please choose an option: ");
            if (menuSelection == 1) {
                viewCurrentBalance();
            } else if (menuSelection == 2) {
                viewTransferHistory();
            } else if (menuSelection == 3) {
                viewPendingRequests();
            } else if (menuSelection == 4) {
                sendBucks();
            } else if (menuSelection == 5) {
                requestBucks();
            } else if (menuSelection == 6) {
                logout();
                break;
            } else if (menuSelection == 0) {
                continue;
            } else {
                System.out.println("Invalid Selection");
            }
            consoleService.pause();
        }
    }

    // Displays the current balance of the user
    private void viewCurrentBalance() {
        BigDecimal balance = transferService.getBalance();
        consoleService.printBalance(balance);
    }

    // Displays transfer history and allows viewing details of a specific transfer
    private void viewTransferHistory() {
        List<Transfer> transfers = transferService.getTransfersForUser();
        consoleService.printTransferHistory(transfers);

        int transferId = consoleService.promptForInt("\nPlease enter transfer ID to view details (0 to cancel): ");
        if (transferId != 0) {
            Transfer transfer = findTransferById(transfers, transferId);
            if (transfer != null) {
                consoleService.printTransferDetails(transfer);
            } else {
                System.out.println("Transfer not found.");
            }
        }
    }

    // Displays pending transfer requests and allows approving/rejecting them
    private void viewPendingRequests() {
        List<Transfer> pendingTransfers = transferService.getPendingTransfers();
        consoleService.printPendingRequests(pendingTransfers);

        int transferId = consoleService.promptForInt("\nPlease enter transfer ID to approve/reject (0 to cancel): ");
        if (transferId != 0) {
            Transfer transfer = findTransferById(pendingTransfers, transferId);
            if (transfer != null) {
                approveOrRejectTransfer(transfer);
            } else {
                System.out.println("Invalid transfer ID or transfer is not pending.");
            }
        }
    }

    // Handles sending TE bucks to another user
    private void sendBucks() {
        List<User> users = transferService.getAllUsers();
        consoleService.printUserList(users);

        int receiverId = consoleService.promptForInt("\nEnter ID of user you are sending to (0 to cancel): ");
        if (receiverId != 0 && receiverId != currentUser.getUser().getId()) {
            BigDecimal amount = consoleService.promptForBigDecimal("Enter amount: ");
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                TransferDto transferDto = new TransferDto();
                transferDto.setReceiverId(receiverId);
                transferDto.setAmount(amount);
                transferDto.setType("Send");
                try {
                    Transfer sentTransfer = transferService.createTransfer(transferDto);
                    System.out.println("Transfer sent successfully. Transfer ID: " + sentTransfer.getTransferId());
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            } else {
                System.out.println("Invalid amount. Please enter a positive amount.");
            }
        } else if (receiverId == currentUser.getUser().getId()) {
            System.out.println("You cannot send money to yourself.");
        }
    }

    // Handles requesting TE bucks from another user
    private void requestBucks() {
        List<User> users = transferService.getAllUsers();
        consoleService.printUserList(users);

        int senderId = consoleService.promptForInt("Enter ID of user you are requesting from (0 to cancel): ");
        if (senderId != 0 && senderId != currentUser.getUser().getId()) {
            BigDecimal amount = consoleService.promptForBigDecimal("Enter amount: ");
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                TransferDto transferDto = new TransferDto();
                transferDto.setReceiverId(senderId);
                transferDto.setAmount(amount);
                transferDto.setType("Request");
                try {
                    Transfer createdTransfer = transferService.requestTransfer(transferDto);
                    System.out.println("Request sent successfully. Transfer ID: " + createdTransfer.getTransferId());
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            } else {
                System.out.println("Invalid amount. Please enter a positive amount.");
            }
        } else if (senderId == currentUser.getUser().getId()) {
            System.out.println("You cannot request money from yourself.");
        }
    }

    // Handles approving or rejecting a pending transfer
    private void approveOrRejectTransfer(Transfer transfer) {
        consoleService.printApproveRejectMenu();
        int choice = consoleService.promptForInt("Please choose an option: ");

        if (choice == 1 || choice == 2) {
            int statusId = (choice == 1) ? 2 : 3; // 2 for Approved, 3 for Rejected
            try {
                transferService.updateTransferStatus(transfer.getTransferId(), statusId);
                System.out.println("Transfer " + (choice == 1 ? "approved" : "rejected") + " successfully.");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else {
            System.out.println("No action taken.");
        }
    }

    // Helper method to find a transfer by its ID in a list of transfers
    private Transfer findTransferById(List<Transfer> transfers, int transferId) {
        return transfers.stream()
                .filter(t -> t.getTransferId() == transferId)
                .findFirst()
                .orElse(null);
    }

    // Handles user logout
    private void logout() {
        currentUser = null;
        transferService.setAuthToken(null);
        System.out.println("You have been logged out. Goodbye!");
    }

    // Main method to start the application
    public static void main(String[] args) {
        App app = new App();
        app.run();
    }
}