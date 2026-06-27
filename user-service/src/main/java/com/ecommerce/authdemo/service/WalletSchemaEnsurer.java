package com.ecommerce.authdemo.service;

/**
 * Ensures wallet tables exist before reads/writes (production DBs without Flyway V6).
 */
public interface WalletSchemaEnsurer {

    void ensureWalletTables();
}
