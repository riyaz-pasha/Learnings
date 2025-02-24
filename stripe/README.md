# Stripe

Stripe commands and apis

---
### Creating tier based prices for products

1. Create a product
    ```bash
    stripe products create --name="Cloudly Basic" --id="cloudly_basic"
    ```
2. Create price and attach it to the product
   ```bash
   stripe prices create --currency=usd \
   --unit-amount=500 \
   --product="cloudly_basic" \
   -d "recurring[interval]"=month
   ```
3. Update product with marketing features
    ```bash
    stripe products update cloudly_basic \
    -d "marketing_features[0][name]=Access to the Cloudly project management dashboard." \
    -d "marketing_features[1][name]=Up to 10 active projects with 5 team members each." \
    -d "marketing_features[2][name]=Built-in reporting and analytics."
    ```

---
1. Create entitlement features
   ```bash
   stripe entitlements features create \
   --name="Project creations" \
   --lookup-key=create-projects-limited
   ```
2. Attach to the product
   ```bash
   stripe product_features create cloudly_basic \
   --entitlement-feature=feat_test_61S2ks74ynjdtgijb41QNrIGfMhzQGjI
   ```
3. Look what customer has
   ```bash
   stripe entitlements active_entitlements list \
   --customer="cus_*"
   ```

---
## Meters API

1. Create
   ```bash
   stripe billing meters create \
   --display-name="Video Conferencing" \
   --event-name=videoconferencing \
   -d "default_aggregation[formula]"=sum \
   -d "value_settings[event_payload_key]"=minutes
   ```
      ```bash
   stripe billing meters create \
   --display-name="Video Conferencing" \
   --event-name=videoconferencing \
   --default-aggregation.formula=sum \
   --value-settings.event-payload-key=minutes
   ```

2. Trigger event
   ```bash
    curl https://api.stripe.com/v1/billing/meter_events \
    -u "sk_test_*:" \
    -d event_name=videoconferencing \
    -d timestamp=1739771583 \
    -d "payload[stripe_customer_id]"="{{CUSTOMER_ID" \
    -d "payload[minutes]"=1
   ```
