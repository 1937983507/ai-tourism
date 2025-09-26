package com.example.aitourism.util;

public class Constants {

    // Common Status Codes
    public static final int STATUS_SUCCESS = 0; // This might be for API response success
    // public static final int STATUS_FAILURE = 1; // Consider renaming or providing more specific failure codes

    // User Status Codes
    public static final int USER_STATUS_INACTIVE = 0;
    public static final int USER_STATUS_ACTIVE = 1;

    // Error Codes
    public static final int ERROR_CODE_ACCOUNT_OR_PASSWORD_INVALID = 1001;
    public static final int ERROR_CODE_TOKEN_EXPIRED = 1101;
    public static final int ERROR_CODE_BAD_REQUEST = 4000;
    public static final int ERROR_CODE_SERVER_ERROR = 5000; // Used for generic server errors, also 500

    // TODO: Add more specific error codes as identified in the project
}
