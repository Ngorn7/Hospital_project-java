# End-to-end API checks for the running backend (http://localhost:8080).
# Needs an admin account: admin / ChangeMe!12345 (or edit the credentials below) and a
# receptionist account rec1 / password1 created by the script. Run: python3 scripts/api-smoke-test.py
import json, urllib.request, urllib.error
B = "http://localhost:8080/api/v1"
def call(method, path, body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(B + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token: req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req) as r:
            raw = r.read().decode()
            return r.status, (json.loads(raw) if raw.startswith(("{", "[")) else raw)
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try: return e.code, json.loads(raw)
        except Exception: return e.code, raw
results = []
def check(label, expected, got_status, body=None):
    ok = got_status == expected
    results.append(ok)
    msg = body.get("message") if isinstance(body, dict) and "message" in body else ""
    print(f"[{'PASS' if ok else 'FAIL'}] {label}: got {got_status} (expected {expected}) {msg}")

_, t = call("POST", "/auth/login", {"usernameOrEmail": "admin", "password": "ChangeMe!12345"})
T = t["accessToken"]
s, d = call("POST", "/departments", {"name": "Cardiology", "location": "B1", "phone": "012"}, T); check("create department", 201, s); D = d["id"]
s, dr = call("POST", "/doctors", {"departmentId": D, "fullName": "Dr A", "specialty": "Heart", "consultationFee": 25}, T); check("create doctor", 201, s); DR = dr["id"]
s, p = call("POST", "/patients", {"fullName": "Pat One", "dateOfBirth": "1990-01-02", "gender": "MALE", "email": ""}, T); check("create patient (blank email ok)", 201, s); P = p["id"]
s, p2 = call("POST", "/patients", {"fullName": "Pat Two", "dateOfBirth": "1991-01-02"}, T)
s, res = call("GET", "/patients?q=Pat&page=0&size=1&sort=fullName,asc", None, T); check("patient search is paged (totalElements=2, size=1)", 200, s)
results.append(res["totalElements"] == 2 and len(res["content"]) == 1); print("   totalElements", res["totalElements"], "content", len(res["content"]))
s, ap = call("POST", "/appointments", {"patientId": P, "doctorId": DR, "appointmentAt": "2026-11-01T09:30:00", "reason": "checkup"}, T); check("schedule appointment", 201, s); AP = ap["id"]
s, b = call("POST", "/appointments", {"patientId": P, "doctorId": DR, "appointmentAt": "2026-11-01T09:30:00"}, T); check("double-book rejected", 409, s, b)
s, b = call("PATCH", f"/appointments/{AP}/status?status=COMPLETED", None, T); check("SCHEDULED->COMPLETED rejected (state machine)", 409, s, b)
s, b = call("PATCH", f"/appointments/{AP}/status?status=CONFIRMED", None, T); check("SCHEDULED->CONFIRMED allowed", 200, s, b)
s, b = call("PATCH", f"/appointments/{AP}/status?status=NOPE", None, T); check("invalid enum rejected", 400, s, b)
s, lst = call("GET", "/appointments?page=0&size=5&sort=appointmentAt,desc", None, T); check("appointments list with sort", 200, s)
s, b = call("GET", "/doctors?sort=nope,asc", None, T); check("unknown sort field -> 400", 400, s, b)
s, rm = call("POST", "/rooms", {"departmentId": D, "roomNumber": "R-101", "roomType": "Ward", "dailyRate": 40, "status": "AVAILABLE"}, T); check("create room", 201, s); R = rm["id"]
s, b = call("PUT", f"/rooms/{R}", {"departmentId": D, "roomNumber": "R-101", "roomType": "Ward", "dailyRate": 40, "status": "OCCUPIED"}, T); check("manual OCCUPIED with no admission rejected", 409, s, b)
s, adm = call("POST", "/admissions", {"patientId": P, "doctorId": DR, "roomId": R, "diagnosis": "flu"}, T); check("admit patient", 201, s); ADM = adm["id"]
s, b = call("PUT", f"/rooms/{R}", {"departmentId": D, "roomNumber": "R-101", "roomType": "Ward", "dailyRate": 45}, T); check("room edit keeps OCCUPIED when status omitted", 200, s, b)
s, b = call("PUT", f"/rooms/{R}", {"departmentId": D, "roomNumber": "R-101", "roomType": "Ward", "dailyRate": 45, "status": "AVAILABLE"}, T); check("cannot free room with active admission", 409, s, b)
s, b = call("PATCH", f"/admissions/{ADM}/discharge", None, T); check("discharge", 200, s, b)
s, rl = call("GET", "/rooms?status=AVAILABLE", None, T); results.append(any(x["id"] == R for x in rl)); print("[%s] discharge releases room %s" % ("PASS" if results[-1] else "FAIL", R))
s, b = call("PATCH", f"/admissions/{ADM}/discharge", None, T); check("second discharge rejected", 409, s, b)
s, b = call("POST", "/bills", {"patientId": P, "admissionId": ADM, "additionalCharges": 10}, T); check("bill discharged admission", 201, s, b)
if s == 201: print("   total", b.get("totalAmount"))
s, b = call("POST", "/bills", {"patientId": P, "admissionId": ADM}, T); check("duplicate admission bill rejected", 409, s, b)
s, ap2 = call("POST", "/appointments", {"patientId": P, "doctorId": DR, "appointmentAt": "2026-12-01T10:00:00"}, T)
s, b = call("PATCH", f"/appointments/{ap2['id']}/status?status=CANCELLED", None, T); check("cancel appointment", 200, s, b)
s, b = call("POST", "/bills", {"patientId": P, "appointmentId": ap2["id"]}, T); check("bill cancelled appointment rejected", 409, s, b)
s, b = call("POST", "/bills", {"patientId": P + 99, "admissionId": ADM}, T); check("bill for another patient's admission rejected", 404, s, b)
s, b = call("POST", "/prescriptions", {"patientId": P, "doctorId": DR, "items": [{"medicineId": 1, "dosage": "x", "frequency": "y", "durationDays": 0, "quantity": 1}]}, T); check("prescription item validation", 400, s, b)
s, b = call("POST", "/auth/register", {"username": "rec1", "email": "rec1@x.com", "password": "password1", "role": "RECEPTIONIST"}, T); check("register receptionist", 201, s, b)
s, b = call("POST", "/auth/register", {"username": "rec1", "email": "other@x.com", "password": "password1", "role": "RECEPTIONIST"}, T); check("duplicate username -> 409", 409, s, b)
_, rt = call("POST", "/auth/login", {"usernameOrEmail": "rec1", "password": "password1"}); RT = rt["accessToken"]
s, b = call("DELETE", f"/patients/{P}", None, RT); check("receptionist cannot delete -> 403", 403, s, b)
s, b = call("GET", "/dashboard", None, RT); check("receptionist can read dashboard", 200, s, b)
s, b = call("GET", "/auth/me", None, None); check("no token -> 401", 401, s, b)
s, b = call("POST", "/auth/login", {"usernameOrEmail": "admin", "password": "wrong"}); check("bad password -> 401", 401, s, b)
print(f"\n{sum(results)}/{len(results)} checks passed")
