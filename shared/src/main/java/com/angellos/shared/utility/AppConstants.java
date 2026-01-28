package com.angellos.shared.utility;

public interface AppConstants {

    /**
     * Context paths
     */
    String CONTEXT_PATH = "digicare/support/";
    String AUTH_CONTEXT_PATH = "/digicare/auth/";
    String TPA_CONTEXT_PATH = "/digicare/tpa/";
    String CBS_CONTEXT_PATH = "/digicare/cbs/";
    String UTILITY_CONTEXT_PATH = "/digicare/utility/";
    String TERI_CONTEXT_PATH = "/digicare/teri/";
    String CRM_CONTEXT_PATH = "/digicare/crm/";
    String PAYMENT_CONTEXT_PATH = "/digicare/payment/";
    String TRADING_CONTEXT_PATH = "/api/v1/trading/";
    String ENDPOINT_PATH = "/query/endpoints";


    /**
     * Messages
     */
    String SUCCESS_MESSAGE = "Request completed successfully!";
    String SAVED_MESSAGE = "Record saved successfully";
    String SAVED_LIST_MESSAGE = "Records saved successfully";
    String UPDATED_MESSAGE = "Record updated successfully";
    String UPDATED_LIST_MESSAGE = "Records updated successfully";
    String FETCH_SUCCESS_MESSAGE = "Records retrieved successfully!";
    String FETCH_SUCCESS_MESSAGE_BY_ID = "Record successfully retrieved by id";
    String NO_RECORD_FOUND = "No record found!";
    String NO_RECORD_FOUND_OR_DISABLED = "No record found or disabled!";
    String DELETED_MESSAGE = "Record deleted successfully";
    String DELETED_LIST_MESSAGE = "Records deleted successfully";
    String ERROR_MESSAGE = "Error Occurred!";
    String NON_NULL_OR_EMPTY = "Cannot be null or empty";
    String NO_AUTHORIZATION_MESSAGE = "No authorization to perform this action";
    String LDAP_ERROR = "Error authenticating on ldap : Invalid username or password provided.";
    String LDAP_USER_QUERY = "firstName,lastName,email,mobile,username,fullName,gender";
    String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";
    String USER_EXISTS = "User already exists";
    String CLIENT_EXISTS = "Client already exists with this name";
    String ROLE_EXISTS = "Role already exists with this name";
    String PERMISSION_EXISTS = "Permission already exists with this name ";
    String PLATFORM_EXISTS = "Platform already exists with this name ";
    String GENERAL_CODE_EXISTS = "General code already exists with this code name ";
    String GENERAL_CODE_TYPE_EXISTS = "General code type already exists with this code name ";
    String ACTION_SUCCESS_MESSAGE = "Action successful!";
    String DATE_VALIDATION = "Start Date cannot be after End Date";
    String UPLOAD_VALIDATION = "Fill all fields to complete upload";

    /**
     * Mail
     */
    String REGISTRATION_MAIL_TITLE = "Welcome to DigiCare Support";
    String MAIL_SENDER = "noreply@telecel.com.gh";


    /**
     * Pagination filtering and sorting params
     */
    String PARAM_PAGINATE = "paginate";
    String PARAM_PAGE_NO = "page";
    String PARAM_PAGE_SIZE = "size";
    String PARAM_SORT_BY = "sortBy";
    String PARAM_SORT_DIR = "sortDir";
    String PARAM_SEARCH = "search";
    String PARAM_STATUS = "status";
    String PARAM_NAME = "name";
    String PARAM_TYPE = "type";
    String PARAM_USER_ID = "userId";
    String START_DATE = "startDate";
    String END_DATE =  "endDate";
    String CLIENT_TYPE =  "clientType";
    String CODE_TYPE =  "codeType";

    String TRANSACTION_TYPES = "transactionTypes";
    String PAYMENT_TYPES = "paymentTypes";
    String FULFILMENT_TYPES = "fulfilmentTypes";
    String PAYMENT_PROCESSORS = "paymentProcessors";
    String PAYMENT_CHANNELS = "paymentChannels";
    String PAYMENT_PLATFORM = "paymentPlatform";
    String CHANNEL_IDS = "channelIds";
    String TRANSACTION_ID = "transactionId";
    String CONVO_ID = "conversationId";
    String ORDER_ID = "orderId";
    String REF_NUMBER = "referenceNumber";
    String FBB_STATUS = "FbbStatus";
    String PAYMENT_STATUS = "paymentStatus";
    String CLIENTS = "clients";
    String SERVICES = "services";


    /**
     * Default values
     */
    Integer MAX_PAGE_SIZE_API = 100;
    String DEFAULT_PAGE_SIZE = "25";
    String DEFAULT_PAGE_SORT_DIR = "asc";
    String DEFAULT_PAGE_SORT_BY = "createdAt";
    String DEFAULT_PAGE_NUMBER = "1";
    String DEFAULT_PAGINATE = "true";
    String DEFAULT_SEARCH = "";
    String DEFAULT_STATUS = "1";
    String DEFAULT_TRANSACTION_TYPE_TP = "PAYMENT";
    String DEFAULT_TRANSACTION_TYPE_WEB = "Linkyfi Pay Bill";
    String DEFAULT_TRANSACTION_TYPE_APP = "check_balance";
    String DEFAULT_FULFILMENT_TYPE = "14";
    String DEFAULT_PAYMENT_PROCESSOR = "PAYSTACK";
    String DEFAULT_PAYMENT_TYPE = "PAYMENT";
    String DEFAULT_PAYMENT_CHANNEL = "";
    String DEFAULT_CHANNEL_ID = "web";
    String DEFAULT_FBB_STATUS = "0";
    String DEFAULT_PAYMENT_STATUS = "FAILED";
    String DEFAULT_CLIENT_TYPE = "payment";
    String DEFAULT_START_DATE = "2025-07-07";
    String DEFAULT_END_DATE = "2025-08-08";

    /**
     * Redis
     */
    String ENDPOINTS_REDIS_PREFIX = "endpoints::";
    String REDIS_GET_LOG = "Reading {} data from redis server.";
    String REDIS_NOT_FOUND = "No information on {} was found in redis.";
    String REDIS_SET_LOG = "Setting {} data on redis server.";
    String REDIS_DELETE_LOG = "Deleting {} data from redis server.";
    String REDIS_USER_KEY_PREFIX = "user::";
    String REDIS_CLIENT_ENDPOINTS_PREFIX = "client_endpoints::";
    String REDIS_GENERAL_PREFIX = "general_code::";

    /**
     * Activity actions
     */
    String VIEW = "VIEWED";
    String CREATE = "CREATED";
    String UPDATE = "UPDATED";
    String UPSERT = "UPSERTED";
    String DELETE = "DELETED";
    String LOGGED_IN = "LOGGED IN";
    String LOGGED_OUT = "LOGGED OUT";

    String CLIENT = "CLIENT";
    String CLIENT_ENDPOINT = "CLIENT_ENDPOINT";
    String SERVICE = "SERVICE";
    String SERVICE_ENDPOINT = "SERVICE ENDPOINTS";
    String USER = "USER";
    String ROLE = "ROLE";
    String PERMISSION = "PERMISSION";
    String ROLE_PERMISSION = "ROLE_PERMISSION";
    String THIRD_PARTY = "THIRD PARTY TRANSACTIONS";
    String PUSH_CASH = "PUSH CASH TRANSACTIONS";
    String VF_CASH = "APP CASH TRANSACTIONS";
    String PAYMENT_ROUTE = "PAYMENT ROUTE";
    String PAYMENT_PLATFORM_ = "PAYMENT PLATFORM";
    String GENERAL_CODE = "GENERAL CODE";
    String GENERAL_CODE_TYPE = "GENERAL CODE TYPE";
    String TMP_PAYMENTS = "TEMPORARY PAYMENTS";
}
