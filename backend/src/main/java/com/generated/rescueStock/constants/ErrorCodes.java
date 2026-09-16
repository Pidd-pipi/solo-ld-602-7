package com.generated.rescueStock.constants;

/** 业务错误码：service 与 controller 各自包装，但错误码集中在此。 */
public final class ErrorCodes {
  public static final String AUTH_REQUIRED = "AUTH_REQUIRED";
  public static final String AUTH_INVALID = "AUTH_INVALID";
  public static final String RBAC_DENIED = "RBAC_DENIED";
  public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
  public static final String RATE_LIMITED = "RATE_LIMITED";

  public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
  public static final String ORDER_DUPLICATE_REQUEST = "ORDER_DUPLICATE_REQUEST";
  public static final String IDEMPOTENT_CONFLICT = "IDEMPOTENT_CONFLICT";
  public static final String ILLEGAL_STATUS_TRANSITION = "ILLEGAL_STATUS_TRANSITION";
  public static final String BATCH_NOT_FOUND = "BATCH_NOT_FOUND";
  public static final String INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
  public static final String BATCH_CONTENDED = "BATCH_CONTENDED";
  public static final String WAREHOUSE_MISMATCH = "WAREHOUSE_MISMATCH";
  public static final String LINE_EMPTY = "LINE_EMPTY";
  public static final String ALLOCATION_MISSING = "ALLOCATION_MISSING";
  public static final String LEDGER_IMBALANCE = "LEDGER_IMBALANCE";
  public static final String CONCURRENT_CONFLICT = "CONCURRENT_CONFLICT";
  public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

  private ErrorCodes() {}
}
