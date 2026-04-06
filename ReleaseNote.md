##################################################
# ekedai-service-2.5.1| 10-Sept-2024
##################################################
--Wafi--
1. Added User Merge controller, service

2. Added trigger to ekedai DB:
`CREATE TRIGGER set_reputation_score
BEFORE INSERT ON user
FOR EACH ROW
IF NEW.channel = 'HELLOSIM' THEN
SET NEW.reputationScore = 0.75;
ELSEIF NEW.channel = 'INTERNAL' AND (NEW.nationality IS NULL OR NEW.nationality <> 'Malaysian') THEN
SET NEW.reputationScore = 0.50;
ELSEIF NEW.channel = 'INTERNAL' AND NEW.nationality = 'Malaysian' THEN
SET NEW.reputationScore = 0.60;
END IF;`


##################################################
# ekedai-service-2.5.0| 6-Sept-2024
##################################################
--Faiq--
1. Added endpoint API of Payment channel based on product variant ID

2. Enable back table in staging 'payment_channel' and 'product_variant_payment_channel'

3. Revamp get category and categories for product

##################################################
# ekedai-service-2.3.9| 20-Feb-2024
##################################################
--Hafiz--
1. Added endpoint API of Discount based on DiscountProductVariant
    (/delete-discount-variant)

2. Added endpoint API of Discount based on DiscountUser
    (/list-user-discount)

3. Changed some of the API endpoint name.
4. Modified error checks on DiscountController.
5. Modify DiscountUser and related API.
6. Added endpoint API of Discount for discounted price calculation
    combined with (/list-user-discount) and changed to (/list-discounted-price) 

##################################################
# ekedai-service-2.3.8| 8-Feb-2024
##################################################
--Hafiz--
1. Added 4 new tables in ekedai_staging
    discount, discount_user, discount_product and discount_product_variant

2. Added related model, repository, service and controller for Discount
3. Added related model, repository and service for DiscountUser
    includes DiscountUserId for composite primary key

4. Added related model for DiscountProduct
5. Added related model, repository and service for DiscountProductVariant
6. Added new enum DiscountStatus and enum CalculationType for discount table
7. Added new enum DiscountUserStatus for discount_user table
8. Added new dao DiscountRequest
9. Modified endpoint API of Discount based on DiscountProductVariant changes
    (/get-all), (/create), (/update), (/update-status)

10. Added endpoint API of Discount based on DiscountUser
    (/get-discount-user), (/create-discount-user), (/update-user-status)

--Faiq--
1. Bill Presentment fix

##################################################
# ekedai-service-2.3.7| 4-Feb-2024
##################################################
--Faiq--
1. Add new db Field value
    xx = extra1,extra2,extra3,extra4,billPhoneNumber
    ALTER TABLE ekedai_staging.`transaction` ADD {xx} varchar(100) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL;

2. send extra value 1-4 parans to wsp
3. add new enum at product_required_info table
ALTER TABLE ekedai_staging.product_required_info MODIFY COLUMN fieldValue enum('ACCNO','MOBILENO','ZONE','REFNO','CONSUMERID','SCNO','BILLMOBILENO','BILLDATE','OFFICENO') CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL NULL;


##################################################
# ekedai-service-2.2.2| 5-Oct-2023
##################################################
---Kumar---
1. CSV Helper remove lines


--Faiq--


##################################################
# ekedai-service-2.2.1| 25-Sept-2023
##################################################
---Kumar---
1. MMPay - Bug fixed added header for mmpay request


--Faiq--


##################################################
# ekedai-service-2.2.0| 06-Sept-2023
##################################################
---Kumar---
1. MMPay Added Refund When Transaction Query update the transaction to failed
2. Added Query Transaction Status when transaction state in pending


--Faiq--


##################################################
# ekedai-service-2.1.2| 30-Aug-2023
##################################################
---Kumar---
1. MMPay update request and response


--Faiq--

##################################################
# ekedai-service-2.1.0| 27-June-2023
##################################################
---Kumar---
1. Added New Request to Order Service After Get Callback
2. Updated Table


--Faiq--



##################################################
# ekedai-service-2.0.2| 7-June-2023
##################################################
---Kumar---
1. Bug Fixed Payment Http Request


--Faiq--


##################################################
# ekedai-service-2.0.2| 7-June-2023
##################################################
---Kumar---
1. Bug Fixed WSP query



##################################################
# ekedai-service-2.0.1| 31-May-2023
##################################################
---Kumar---
1. Change Db Field value
ALTER TABLE ekedai_staging.`transaction` CHANGE clientTransactionId otpReferenceNo varchar(100) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL NULL;
ALTER TABLE ekedai_staging.`transaction` CHANGE atxTransactionId spTransactionId varchar(100) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL NULL;

2. Added MMPayment Service
3. Query Operation Added.
4. Added Retail Price Converter
5. Transaction History Api Added

--Faiq--