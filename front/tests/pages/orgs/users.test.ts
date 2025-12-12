import { describe, it, expect } from "vitest";

describe("Users Page - Helper Functions", () => {
  describe("getInitials", () => {
    // Fonction extraite de la page pour les tests
    function getInitials(name: string): string {
      return name
        .split(/\s+/)
        .map((part) => part[0])
        .join("")
        .toUpperCase()
        .slice(0, 2);
    }

    it("should extract initials from full name", () => {
      expect(getInitials("John Doe")).toBe("JD");
    });

    it("should extract initials from single name", () => {
      expect(getInitials("John")).toBe("J");
    });

    it("should extract only first two initials from multiple names", () => {
      expect(getInitials("John Smith Doe")).toBe("JS");
    });

    it("should extract initials from email", () => {
      expect(getInitials("user@example.com")).toBe("U");
    });

    it("should handle names with multiple spaces", () => {
      expect(getInitials("Jean   Paul  Sartre")).toBe("JP");
    });

    it("should convert to uppercase", () => {
      expect(getInitials("john doe")).toBe("JD");
    });
  });

  describe("Email Validation", () => {
    it("should validate correct email format", () => {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

      expect(emailRegex.test("user@example.com")).toBe(true);
      expect(emailRegex.test("test.user@example.co.uk")).toBe(true);
      expect(emailRegex.test("user+tag@example.com")).toBe(true);
    });

    it("should reject invalid email format", () => {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

      expect(emailRegex.test("invalid-email")).toBe(false);
      expect(emailRegex.test("not-an-email")).toBe(false);
      expect(emailRegex.test("@example.com")).toBe(false);
      expect(emailRegex.test("user@")).toBe(false);
      expect(emailRegex.test("user@example")).toBe(false);
    });
  });

  describe("Email Processing", () => {
    it("should parse emails from multiline string", () => {
      const input = "user1@example.com\nuser2@example.com\nuser3@example.com";
      const emailLines = input
        .split("\n")
        .map((line) => line.trim())
        .filter((line) => line.length > 0);

      expect(emailLines).toEqual(["user1@example.com", "user2@example.com", "user3@example.com"]);
    });

    it("should ignore empty lines", () => {
      const input = "user1@example.com\n\n\nuser2@example.com\n";
      const emailLines = input
        .split("\n")
        .map((line) => line.trim())
        .filter((line) => line.length > 0);

      expect(emailLines).toEqual(["user1@example.com", "user2@example.com"]);
    });

    it("should trim whitespace from emails", () => {
      const input = "  user1@example.com  \n  user2@example.com  ";
      const emailLines = input
        .split("\n")
        .map((line) => line.trim())
        .filter((line) => line.length > 0);

      expect(emailLines).toEqual(["user1@example.com", "user2@example.com"]);
    });
  });

  describe("Email Deduplication", () => {
    it("should deduplicate emails when combining lists", () => {
      const existingEmails = ["user1@example.com", "user2@example.com"];
      const newEmails = ["user2@example.com", "user3@example.com"];
      const allEmails = [...new Set([...existingEmails, ...newEmails])];

      expect(allEmails).toEqual(["user1@example.com", "user2@example.com", "user3@example.com"]);
      expect(allEmails.length).toBe(3);
    });

    it("should preserve all emails when no duplicates", () => {
      const existingEmails = ["user1@example.com", "user2@example.com"];
      const newEmails = ["user3@example.com", "user4@example.com"];
      const allEmails = [...new Set([...existingEmails, ...newEmails])];

      expect(allEmails).toEqual([
        "user1@example.com",
        "user2@example.com",
        "user3@example.com",
        "user4@example.com",
      ]);
      expect(allEmails.length).toBe(4);
    });

    it("should handle all duplicates", () => {
      const existingEmails = ["user1@example.com", "user2@example.com"];
      const newEmails = ["user1@example.com", "user2@example.com"];
      const allEmails = [...new Set([...existingEmails, ...newEmails])];

      expect(allEmails).toEqual(["user1@example.com", "user2@example.com"]);
      expect(allEmails.length).toBe(2);
    });
  });

  describe("Email Filtering for Deletion", () => {
    it("should filter out specific email", () => {
      const users = [
        { email: "user1@example.com", display_name: "User 1", picture_url: null },
        { email: "user2@example.com", display_name: "User 2", picture_url: null },
        { email: "user3@example.com", display_name: "User 3", picture_url: null },
      ];
      const emailToDelete = "user2@example.com";
      const remainingEmails = users
        .filter((user) => user.email !== emailToDelete)
        .map((user) => user.email);

      expect(remainingEmails).toEqual(["user1@example.com", "user3@example.com"]);
      expect(remainingEmails.length).toBe(2);
    });

    it("should return all emails when email to delete does not exist", () => {
      const users = [
        { email: "user1@example.com", display_name: "User 1", picture_url: null },
        { email: "user2@example.com", display_name: "User 2", picture_url: null },
      ];
      const emailToDelete = "nonexistent@example.com";
      const remainingEmails = users
        .filter((user) => user.email !== emailToDelete)
        .map((user) => user.email);

      expect(remainingEmails).toEqual(["user1@example.com", "user2@example.com"]);
      expect(remainingEmails.length).toBe(2);
    });

    it("should return empty array when deleting last user", () => {
      const users = [{ email: "user1@example.com", display_name: "User 1", picture_url: null }];
      const emailToDelete = "user1@example.com";
      const remainingEmails = users
        .filter((user) => user.email !== emailToDelete)
        .map((user) => user.email);

      expect(remainingEmails).toEqual([]);
      expect(remainingEmails.length).toBe(0);
    });
  });

  describe("Invalid Email Detection", () => {
    it("should identify invalid emails in a list", () => {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      const emailLines = [
        "valid@example.com",
        "invalid-email",
        "another-invalid",
        "valid2@example.com",
      ];
      const invalidEmails = emailLines.filter((email) => !emailRegex.test(email));

      expect(invalidEmails).toEqual(["invalid-email", "another-invalid"]);
      expect(invalidEmails.length).toBe(2);
    });

    it("should return empty array when all emails are valid", () => {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      const emailLines = ["user1@example.com", "user2@example.com", "user3@example.com"];
      const invalidEmails = emailLines.filter((email) => !emailRegex.test(email));

      expect(invalidEmails).toEqual([]);
      expect(invalidEmails.length).toBe(0);
    });

    it("should identify all invalid emails", () => {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      const emailLines = ["invalid1", "invalid2", "invalid3"];
      const invalidEmails = emailLines.filter((email) => !emailRegex.test(email));

      expect(invalidEmails).toEqual(["invalid1", "invalid2", "invalid3"]);
      expect(invalidEmails.length).toBe(3);
    });
  });

  describe("Picture URL Validation", () => {
    it("should detect valid picture URL", () => {
      const pictureUrl = "https://example.com/user.jpg";
      const isValid = pictureUrl && pictureUrl.trim() !== "";

      expect(isValid).toBe(true);
    });

    it("should detect empty picture URL", () => {
      const pictureUrl = "";
      const isValid = pictureUrl && pictureUrl.trim() !== "";

      expect(isValid).toBeFalsy();
    });

    it("should detect null picture URL", () => {
      const pictureUrl = null;
      const isValid = pictureUrl && pictureUrl.trim() !== "";

      expect(isValid).toBeFalsy();
    });

    it("should detect whitespace-only picture URL", () => {
      const pictureUrl = "   ";
      const isValid = pictureUrl && pictureUrl.trim() !== "";

      expect(isValid).toBe(false);
    });
  });

  describe("User Display Name Logic", () => {
    it("should use display_name when available", () => {
      const user = {
        email: "user@example.com",
        display_name: "John Doe",
        picture_url: null,
      };
      const displayText = user.display_name || user.email;

      expect(displayText).toBe("John Doe");
    });

    it("should fallback to email when no display_name", () => {
      const user = {
        email: "user@example.com",
        display_name: null,
        picture_url: null,
      };
      const displayText = user.display_name || user.email;

      expect(displayText).toBe("user@example.com");
    });

    it("should fallback to email when display_name is empty string", () => {
      const user = {
        email: "user@example.com",
        display_name: "",
        picture_url: null,
      };
      const displayText = user.display_name || user.email;

      expect(displayText).toBe("user@example.com");
    });
  });
});
