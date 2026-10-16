#!/bin/bash

TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"smasobur","password":"-1sikDar1-"}' | jq -r '.data.token')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo "❌ Failed to get token. Check your credentials."
    exit 1
fi

echo "✅ Got token"
echo "Fetching species list..."

# Fetch species — note the response shape: { data: [ { id, code, name }, ... ] }
SPECIES_JSON=$(curl -s "http://localhost:8080/api/species?activeOnly=true" \
  -H "Authorization: Bearer $TOKEN")

# Extract id + code pairs into parallel arrays
SPECIES_IDS=($(echo "$SPECIES_JSON" | jq -r '.data[].id'))
SPECIES_CODES=($(echo "$SPECIES_JSON" | jq -r '.data[].code'))

echo "Found ${#SPECIES_IDS[@]} species"
echo "Starting to seed 100 animals..."

# Sample names — mixed Bangladeshi
NAMES=("Lakshmi" "Roni" "Bitu" "Tommy" "Goru" "Kalo" "Dhola" "Sundari" "Rani" "Chotu"
       "Bhola" "Kajol" "Mithu" "Pintu" "Bulu" "Tulu" "Rupa" "Shona" "Motu" "Chiku"
       "Raju" "Babu" "Mama" "Kaka" "Chacha" "Dada" "Nana" "Bhai" "Bon" "Kobutor")

STATUSES=("ACTIVE" "ACTIVE" "ACTIVE" "ACTIVE" "SOLD" "DEAD")   # weighted toward ACTIVE
GENDERS=("FEMALE" "FEMALE" "FEMALE" "MALE")                     # weighted toward FEMALE

SUCCESS=0
FAIL=0

for i in $(seq 1 100); do
    # Pick random values
    SPECIES_INDEX=$((RANDOM % ${#SPECIES_IDS[@]}))
    SPECIES_ID=${SPECIES_IDS[$SPECIES_INDEX]}
    SPECIES_CODE=${SPECIES_CODES[$SPECIES_INDEX]}

    NAME=${NAMES[$((RANDOM % ${#NAMES[@]}))]}
    STATUS=${STATUSES[$((RANDOM % ${#STATUSES[@]}))]}
    GENDER=${GENDERS[$((RANDOM % ${#GENDERS[@]}))]}

    # Random date of birth between 1 and 5 years ago
    YEAR=$((2021 + RANDOM % 5))
    MONTH=$((1 + RANDOM % 12))
    DAY=$((1 + RANDOM % 28))
    DOB=$(printf "%04d-%02d-%02d" $YEAR $MONTH $DAY)

    # Tag number: SPECIES-####
    TAG="${SPECIES_CODE}-$(printf '%03d' $i)"

    RESPONSE=$(curl -s -X POST http://localhost:8080/api/animals \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"tagNumber\": \"$TAG\",
        \"name\": \"$NAME\",
        \"speciesId\": $SPECIES_ID,
        \"gender\": \"$GENDER\",
        \"lifecycleStatus\": \"$STATUS\",
        \"dateOfBirth\": \"$DOB\"
      }")

    SUCCESS_FLAG=$(echo "$RESPONSE" | jq -r '.success')

    if [ "$SUCCESS_FLAG" = "true" ]; then
        SUCCESS=$((SUCCESS + 1))
        printf "."
    else
        FAIL=$((FAIL + 1))
        echo ""
        echo "❌ Failed to create $TAG: $(echo "$RESPONSE" | jq -r '.message')"
    fi
done

echo ""
echo "======================================="
echo "✅ Created: $SUCCESS"
echo "❌ Failed:  $FAIL"
echo "======================================="