package com.ecommerce.authservice.entity;

/**
 * App roles, carried in the token's {@code roles} claim. Lowercase to match the Entra External ID
 * app-role values, so both token issuers look the same to the gateway (docs ADR 0007).
 */
public final class Roles {

  /** Everyone who signs up. Sign-up never grants anything higher. */
  public static final String CUSTOMER = "customer";

  /** Handles orders. Only ever assigned, never self-selected. */
  public static final String STAFF = "staff";

  /** Manages the catalogue and prices. Only ever assigned, never self-selected. */
  public static final String ADMIN = "admin";

  private Roles() {}
}
