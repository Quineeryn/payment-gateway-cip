CREATE TYPE channel_enum AS ENUM (
    'MOBILE_BANKING',
    'INTERNET_BANKING',
    'ATM'
);

CREATE TYPE transaction_status_enum AS ENUM (
    'PENDING',
    'SUCCESS',
    'FAILED'
);

CREATE TABLE transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        VARCHAR(255) NOT NULL UNIQUE,
    channel         channel_enum NOT NULL,
    amount          NUMERIC(19, 2) NOT NULL,
    account         VARCHAR(255) NOT NULL,
    currency        VARCHAR(10) NOT NULL DEFAULT 'IDR',
    payment_method  VARCHAR(100) NOT NULL,
    status          transaction_status_enum NOT NULL DEFAULT 'PENDING',
    corebank_reference  VARCHAR(255),
    biller_reference    VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_order_id ON transactions(order_id);
CREATE INDEX idx_transactions_status ON transactions(status);