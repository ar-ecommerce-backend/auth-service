-- ADR 0007: roles are customer / staff / admin. Everyone who signed up before was a customer.
UPDATE users SET role = 'customer' WHERE role = 'USER';
