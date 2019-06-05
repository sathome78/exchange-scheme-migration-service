-- before invoke this migration you have to delete duplicate records

CREATE UNIQUE INDEX INTERNAL_WALLET_BALANCES_currency_id_role_id__index ON INTERNAL_WALLET_BALANCES (currency_id, role_id);