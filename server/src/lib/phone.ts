/**
 * Saudi numbers arrive in every shape a keyboard allows: 0512345678,
 * 512345678, +966512345678, 00966512345678, with spaces or dashes. They all
 * mean the same person, so they are normalised to one canonical E.164 form
 * before anything touches the database — otherwise the same human ends up with
 * several accounts.
 */

const SAUDI_COUNTRY_CODE = "966";
/** Saudi mobile numbers are 9 digits and always start with 5. */
const SAUDI_MOBILE_LENGTH = 9;

export class InvalidPhoneError extends Error {
  constructor(input: string) {
    super(`"${input}" is not a valid Saudi mobile number`);
    this.name = "InvalidPhoneError";
  }
}

/** Returns +9665XXXXXXXX, or throws. */
export function normaliseSaudiPhone(input: string): string {
  const digits = input.replace(/[^\d+]/g, "").replace(/^\+/, "");

  let national = digits;
  if (national.startsWith("00" + SAUDI_COUNTRY_CODE)) {
    national = national.slice(2 + SAUDI_COUNTRY_CODE.length);
  } else if (national.startsWith(SAUDI_COUNTRY_CODE)) {
    national = national.slice(SAUDI_COUNTRY_CODE.length);
  } else if (national.startsWith("0")) {
    national = national.slice(1);
  }

  if (national.length !== SAUDI_MOBILE_LENGTH || !national.startsWith("5")) {
    throw new InvalidPhoneError(input);
  }
  return `+${SAUDI_COUNTRY_CODE}${national}`;
}

export function isValidSaudiPhone(input: string): boolean {
  try {
    normaliseSaudiPhone(input);
    return true;
  } catch {
    return false;
  }
}

/** Masked form for logs and error messages: +96650****678 */
export function maskPhone(e164: string): string {
  if (e164.length < 8) return "****";
  return `${e164.slice(0, 7)}****${e164.slice(-3)}`;
}
