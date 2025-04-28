import requests

refresh_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIxMmE3YjQ2My1mNTRiLTRlNDgtOGIxMy0wY2Y0M2I5MzUxMzciLCJpYXRDdXN0b20iOiIxNzQ1ODY2NDU2NDU3IiwiaWF0IjoxNzQ1ODY2NDU2fQ.g4V5rgF-g1itwwR83lSzZ4VO8rQjel0IAef4Udk6x0Q"

BASE_URL = "https://backend-orvio.1110777.xyz"

# Step 1: Refresh token
refresh_response = requests.post(
    f"{BASE_URL}/auth/refresh",
    json={"refreshToken": refresh_token},
    headers={"Content-Type": "application/json"}
)
print(refresh_response.json())
access_token = refresh_response.json()["accessToken"]

# Step 2: Send OTP
otp_response = requests.post(
    f"{BASE_URL}/service/sendOtp",
    json={"phoneNumber": "9770483089"},
    headers={
        "Content-Type": "application/json",
        "Authorization": f"Bearer {access_token}"
    }
)

print(otp_response.status_code)
print(otp_response.text)

