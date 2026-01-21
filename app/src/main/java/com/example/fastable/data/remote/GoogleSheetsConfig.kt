package com.example.fastable.data.remote

object GoogleSheetsConfig {
    // Google Sheets URL
    const val SHEET_URL = "https://docs.google.com/spreadsheets/d/1ZQJqdArlwCS965uw4sbJrB6j8rEPfZerMT7X8qkXSzY/edit?gid=1882612924#gid=1882612924"

    // Extract spreadsheet ID from URL
    val SPREADSHEET_ID: String
        get() = SHEET_URL.split("/d/")[1].split("/")[0]

    // Service Account Credentials
    const val TYPE = "service_account"
    const val PROJECT_ID = "time-table-project-450013"
    const val PRIVATE_KEY_ID = "d1f3b8e418b1fc204c80a7a00cfd2b55a8695017"
    const val PRIVATE_KEY = """-----BEGIN PRIVATE KEY-----
MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC7Gp4+xHPlQPJL
G3VGg2r4WHosjiheeADEZiLw5hyBSYva9TxfIS+gWEICCQTCc4dhCyBH/Ukb7Okw
wpvUVRjYlWp/zbYTXFDx1tGquxC/WHk8b2CsNUi/2682HbpBmlSpJqaYuxbbMSsI
7U0e2R6oxbVIygSSxxG7x2H5E6lrQLjPCOGTgPdCyHpjj4NmjAA7beonefTB3nXM
MxMjNldfrhJ3V9MC/mMFOtWJaj6beYvIxId6m+9iuBogdzHkYmhMymZ1JP5I4GO7
iVrbYB2IsjJTsnl/x4rXEd748Fg3xlgh+QpUkCkOG9j3KH9b9LukMM852o+IAJ3S
Ob6ctbHFAgMBAAECggEAJloO7MvE+82DvLx8nf8LGqu8I0ziXnbXpWpQKDPqzN9/
8NpKzS8WvZXJtfQWSyt2KQCoVclHxpcZt3p0iaIFzUNXSKooc7B9EQ1Y/deJV8dx
Vl94H+RuLJGBySRvzMmvJ9r51B2pUjWyXgqSP8v+elbIUYrDRDjU3DpCzVTn6clJ
ObH1LWViHEDyvBxVRYK1N+DdpLRJaisfw8hJsm7xvHO6Gc2XW27y3/gYCl4PgsYx
dUFCtZaCJSSdO9nqcEVWZK2yBP4m5iRwR/KWOvrg/yxy+n5IkLtZQ5ED69edpA5V
Hyr8XuC8ZQZ9UwDQm/WwZGqLnUclPdRq8ScUbxIe8QKBgQDf20Yd1Swu76wNV8GI
AZZbfS+l8FlB9b+5Y2jZqh3jeVEI7kG7wotrJSwpEVwsgATb3zQ+Ffpm/tKkNMMF
VBL1wQimqdK/Bf/fm5Q+os4GamtTTlFyylNXeV/cBgRTalzwsJqRh+RovPAPJ2s1
4T2SKj5h3XHqS+Llk3IdLqQELQKBgQDV+FwKituGFJp/JiPOhLy51mCKLmHI53TT
lZAe3//u0YQVbSejnUnFYr17XDh0mJuRdeTckmkv0hjZiA2ByrbbMjxUGbfD0xLb
MCr2InZwdKmaC7+fi1l2x/rC334oC3OnPFAQbYShw5S6R8G3MaB32HhWyDB0vFPT
j9ZvpB9q+QKBgEo7CBE0cyZNS5xREVfsTtOfu4EnJjH9L8pl8IrdInQf8oMnnpyI
cnrhJLepjgsjmHjglw5Pc21b6rWQ2WqW6oKbtCawAbZeYu7fRFVQ30i5WUWSnueV
t/U1xlfLlvuiNZeKuHaxvUgN/vzHcYG4YxZo8664I+Ixr9e5AQo0QScxAoGAILN0
XagbJMLBWe1aS5W9wikhV/z+tNWq5StWe2GAm98pcJzeEgNX4vLUQqY1epxYKkL6
VzuJF+XkJlrEtbFlgNqMnc3QZ/06RIV4C2X48/bgdMqW3qtNYPnvORkvDq+xXT26
fsg+HPrnIBEXaggLnkVXHuw5e53MseipvSY4JwECgYBAhy/9OXmC8ADXjaRtdgLr
Gqr8WisuwQQRCmlaku9OJpKky9/2uI1mvwMXMExWXfq4yZDopw6qSm8PSv5Xs2Ft
5W+YEsOEiqh0pu/QfTd1Dh7GNnbWEERl3RBj6EyUGxINwdL+TcwDDfpLlF3q0nls
TCFebCl7qM0DA8AujtR9Ag==
-----END PRIVATE KEY-----
"""
    const val CLIENT_EMAIL = "timetable-bot-876@time-table-project-450013.iam.gserviceaccount.com"
    const val CLIENT_ID = "108757805364038870086"
    const val AUTH_URI = "https://accounts.google.com/o/oauth2/auth"
    const val TOKEN_URI = "https://oauth2.googleapis.com/token"
    const val AUTH_PROVIDER_CERT_URL = "https://www.googleapis.com/oauth2/v1/certs"
    const val CLIENT_CERT_URL = "https://www.googleapis.com/robot/v1/metadata/x509/timetable-bot-876%40time-table-project-450013.iam.gserviceaccount.com"
    const val UNIVERSE_DOMAIN = "googleapis.com"

    // Sheet names
    val TIMETABLE_SHEETS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
}

