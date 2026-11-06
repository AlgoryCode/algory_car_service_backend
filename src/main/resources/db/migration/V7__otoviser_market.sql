-- OtoViser marketplace: customers book services from businesses listed on the platform.
-- Customers and providers sign up on their own; only the workshop's own staff belong to a business UUID up front.
ALTER TABLE users ALTER COLUMN business_uuid DROP NOT NULL;

CREATE TABLE businesses
(
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID                     NOT NULL UNIQUE,
    owner_id      BIGINT                   NOT NULL UNIQUE REFERENCES users (id),
    name          VARCHAR(120)             NOT NULL,
    description   VARCHAR(1000),
    phone         VARCHAR(20)              NOT NULL,
    city          VARCHAR(60)              NOT NULL,
    district      VARCHAR(60)              NOT NULL,
    address       VARCHAR(300)             NOT NULL,
    slot_capacity INT                      NOT NULL DEFAULT 1,
    active        BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE business_categories
(
    business_id BIGINT      NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    category    VARCHAR(30) NOT NULL,
    PRIMARY KEY (business_id, category)
);

CREATE TABLE bookings
(
    id           BIGSERIAL PRIMARY KEY,
    business_id  BIGINT                   NOT NULL REFERENCES businesses (id),
    customer_id  BIGINT                   NOT NULL REFERENCES users (id),
    category     VARCHAR(30)              NOT NULL,
    booking_date DATE                     NOT NULL,
    start_time   TIME                     NOT NULL,
    plate        VARCHAR(16)              NOT NULL,
    vehicle      VARCHAR(100),
    note         VARCHAR(500),
    status       VARCHAR(20)              NOT NULL,
    status_note  VARCHAR(300),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_bookings_business_day ON bookings (business_id, booking_date);
CREATE INDEX idx_bookings_customer ON bookings (customer_id);
