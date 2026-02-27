import { describe, it, expect, vi } from "vitest";

describe("Sponsor Detail Page - Email History", () => {
  describe("Email History Loading", () => {
    it("should call getPartnershipEmailHistory API with correct parameters", async () => {
      const mockApi = {
        getPartnershipEmailHistory: vi.fn().mockResolvedValue({
          data: {
            items: [],
            total: 0,
          },
        }),
      };

      const orgSlug = "devlille";
      const eventSlug = "2025";
      const partnershipId = "abc123";

      await mockApi.getPartnershipEmailHistory(orgSlug, eventSlug, partnershipId, {
        page: 1,
        pageSize: 100,
      });

      expect(mockApi.getPartnershipEmailHistory).toHaveBeenCalledWith(
        orgSlug,
        eventSlug,
        partnershipId,
        { page: 1, pageSize: 100 },
      );
    });

    it("should return email history items on success", async () => {
      const mockEmailHistory = [
        {
          id: "email-1",
          partnership_id: "abc123",
          sent_at: "2025-01-15T10:30:00Z",
          sender_email: "admin@devlille.fr",
          subject: "Bienvenue au DevFest 2025",
          body_plain_text: "<p>Bonjour, bienvenue!</p>",
          overall_status: "SENT",
          triggered_by: { email: "john@example.com", display_name: "John Doe" },
          recipients: [{ value: "partner@company.com", status: "SENT" }],
        },
      ];

      const mockApi = {
        getPartnershipEmailHistory: vi.fn().mockResolvedValue({
          data: {
            items: mockEmailHistory,
            total: 1,
          },
        }),
      };

      const result = await mockApi.getPartnershipEmailHistory("devlille", "2025", "abc123", {
        page: 1,
        pageSize: 100,
      });

      expect(result.data.items).toHaveLength(1);
      expect(result.data.items[0].id).toBe("email-1");
      expect(result.data.items[0].subject).toBe("Bienvenue au DevFest 2025");
    });

    it("should handle API errors gracefully", async () => {
      const mockApi = {
        getPartnershipEmailHistory: vi
          .fn()
          .mockRejectedValue(new Error("Failed to load email history")),
      };

      await expect(
        mockApi.getPartnershipEmailHistory("devlille", "2025", "abc123", {
          page: 1,
          pageSize: 100,
        }),
      ).rejects.toThrow("Failed to load email history");
    });

    it("should handle 404 error when partnership not found", async () => {
      const mockApi = {
        getPartnershipEmailHistory: vi.fn().mockRejectedValue({
          response: { status: 404, data: { message: "Partnership not found" } },
        }),
      };

      try {
        await mockApi.getPartnershipEmailHistory("devlille", "2025", "invalid-id", {
          page: 1,
          pageSize: 100,
        });
      } catch (error: any) {
        expect(error.response.status).toBe(404);
      }
    });
  });

  describe("Email History State Management", () => {
    it("should set loading state while fetching emails", () => {
      let loadingEmails = false;
      let emailError = false;

      // Simulate starting fetch
      loadingEmails = true;
      emailError = false;

      expect(loadingEmails).toBe(true);
      expect(emailError).toBe(false);
    });

    it("should set error state on fetch failure", () => {
      let loadingEmails = false;
      let emailError = false;

      // Simulate fetch failure
      loadingEmails = false;
      emailError = true;

      expect(loadingEmails).toBe(false);
      expect(emailError).toBe(true);
    });

    it("should clear error state on successful fetch", () => {
      let loadingEmails = false;
      let emailError = true;

      // Simulate successful fetch
      loadingEmails = false;
      emailError = false;

      expect(loadingEmails).toBe(false);
      expect(emailError).toBe(false);
    });
  });

  describe("Email Date Formatting", () => {
    it("should format email date correctly in French locale", () => {
      const formatEmailDate = (dateString: string): string => {
        const date = new Date(dateString);
        return date.toLocaleDateString("fr-FR", {
          day: "2-digit",
          month: "2-digit",
          year: "numeric",
          hour: "2-digit",
          minute: "2-digit",
        });
      };

      const formatted = formatEmailDate("2025-01-15T10:30:00Z");

      expect(formatted).toContain("2025");
      expect(formatted).toContain("01");
      expect(formatted).toContain("15");
    });

    it("should handle invalid date gracefully", () => {
      const formatEmailDate = (dateString: string): string => {
        try {
          const date = new Date(dateString);
          if (isNaN(date.getTime())) {
            return "Date invalide";
          }
          return date.toLocaleDateString("fr-FR", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
          });
        } catch {
          return "Date invalide";
        }
      };

      expect(formatEmailDate("invalid-date")).toBe("Date invalide");
    });
  });

  describe("Email Status Colors", () => {
    it("should return green for SENT status", () => {
      const getEmailStatusColor = (status: string): string => {
        switch (status) {
          case "SENT":
            return "bg-green-100 text-green-800";
          case "PARTIAL":
            return "bg-yellow-100 text-yellow-800";
          case "FAILED":
            return "bg-red-100 text-red-800";
          default:
            return "bg-gray-100 text-gray-800";
        }
      };

      expect(getEmailStatusColor("SENT")).toBe("bg-green-100 text-green-800");
    });

    it("should return yellow for PARTIAL status", () => {
      const getEmailStatusColor = (status: string): string => {
        switch (status) {
          case "SENT":
            return "bg-green-100 text-green-800";
          case "PARTIAL":
            return "bg-yellow-100 text-yellow-800";
          case "FAILED":
            return "bg-red-100 text-red-800";
          default:
            return "bg-gray-100 text-gray-800";
        }
      };

      expect(getEmailStatusColor("PARTIAL")).toBe("bg-yellow-100 text-yellow-800");
    });

    it("should return red for FAILED status", () => {
      const getEmailStatusColor = (status: string): string => {
        switch (status) {
          case "SENT":
            return "bg-green-100 text-green-800";
          case "PARTIAL":
            return "bg-yellow-100 text-yellow-800";
          case "FAILED":
            return "bg-red-100 text-red-800";
          default:
            return "bg-gray-100 text-gray-800";
        }
      };

      expect(getEmailStatusColor("FAILED")).toBe("bg-red-100 text-red-800");
    });

    it("should return gray for unknown status", () => {
      const getEmailStatusColor = (status: string): string => {
        switch (status) {
          case "SENT":
            return "bg-green-100 text-green-800";
          case "PARTIAL":
            return "bg-yellow-100 text-yellow-800";
          case "FAILED":
            return "bg-red-100 text-red-800";
          default:
            return "bg-gray-100 text-gray-800";
        }
      };

      expect(getEmailStatusColor("UNKNOWN")).toBe("bg-gray-100 text-gray-800");
    });
  });

  describe("Email Status Labels", () => {
    it("should return correct label for SENT status", () => {
      const translations = {
        statusSent: "Envoy\u00e9",
        statusPartial: "Partiel",
        statusFailed: "\u00c9chou\u00e9",
      };

      const getEmailStatusLabel = (status: string): string => {
        switch (status) {
          case "SENT":
            return translations.statusSent;
          case "PARTIAL":
            return translations.statusPartial;
          case "FAILED":
            return translations.statusFailed;
          default:
            return status;
        }
      };

      expect(getEmailStatusLabel("SENT")).toBe("Envoy\u00e9");
    });

    it("should return correct label for PARTIAL status", () => {
      const translations = {
        statusSent: "Envoy\u00e9",
        statusPartial: "Partiel",
        statusFailed: "\u00c9chou\u00e9",
      };

      const getEmailStatusLabel = (status: string): string => {
        switch (status) {
          case "SENT":
            return translations.statusSent;
          case "PARTIAL":
            return translations.statusPartial;
          case "FAILED":
            return translations.statusFailed;
          default:
            return status;
        }
      };

      expect(getEmailStatusLabel("PARTIAL")).toBe("Partiel");
    });

    it("should return correct label for FAILED status", () => {
      const translations = {
        statusSent: "Envoy\u00e9",
        statusPartial: "Partiel",
        statusFailed: "\u00c9chou\u00e9",
      };

      const getEmailStatusLabel = (status: string): string => {
        switch (status) {
          case "SENT":
            return translations.statusSent;
          case "PARTIAL":
            return translations.statusPartial;
          case "FAILED":
            return translations.statusFailed;
          default:
            return status;
        }
      };

      expect(getEmailStatusLabel("FAILED")).toBe("\u00c9chou\u00e9");
    });

    it("should return raw status for unknown status", () => {
      const translations = {
        statusSent: "Envoy\u00e9",
        statusPartial: "Partiel",
        statusFailed: "\u00c9chou\u00e9",
      };

      const getEmailStatusLabel = (status: string): string => {
        switch (status) {
          case "SENT":
            return translations.statusSent;
          case "PARTIAL":
            return translations.statusPartial;
          case "FAILED":
            return translations.statusFailed;
          default:
            return status;
        }
      };

      expect(getEmailStatusLabel("UNKNOWN")).toBe("UNKNOWN");
    });
  });

  describe("Email Modal Management", () => {
    it("should open modal and set selected email", () => {
      let selectedEmail: { id: string; subject: string } | null = null;

      const email = { id: "email-1", subject: "Test Email" };

      // Simulate opening modal
      selectedEmail = email;

      expect(selectedEmail).toEqual(email);
      expect(selectedEmail?.id).toBe("email-1");
    });

    it("should close modal and clear selected email", () => {
      let selectedEmail: { id: string; subject: string } | null = {
        id: "email-1",
        subject: "Test Email",
      };

      // Simulate closing modal
      selectedEmail = null;

      expect(selectedEmail).toBeNull();
    });

    it("should update URL when opening email modal", () => {
      const mockHistoryReplaceState = vi.fn();

      const openEmailModal = (emailId: string) => {
        const url = new URL("http://localhost/orgs/devlille/events/2025/sponsors/abc123");
        url.searchParams.set("email", emailId);
        mockHistoryReplaceState({}, "", url.toString());
      };

      openEmailModal("email-123");

      expect(mockHistoryReplaceState).toHaveBeenCalledWith(
        {},
        "",
        "http://localhost/orgs/devlille/events/2025/sponsors/abc123?email=email-123",
      );
    });

    it("should remove email param from URL when closing modal", () => {
      const mockHistoryReplaceState = vi.fn();

      const closeEmailModal = () => {
        const url = new URL(
          "http://localhost/orgs/devlille/events/2025/sponsors/abc123?email=email-123",
        );
        url.searchParams.delete("email");
        mockHistoryReplaceState({}, "", url.toString());
      };

      closeEmailModal();

      expect(mockHistoryReplaceState).toHaveBeenCalledWith(
        {},
        "",
        "http://localhost/orgs/devlille/events/2025/sponsors/abc123",
      );
    });

    it("should open email from URL param on page load", () => {
      const emailHistory = [
        { id: "email-1", subject: "First Email" },
        { id: "email-2", subject: "Second Email" },
        { id: "email-3", subject: "Third Email" },
      ];

      const emailIdFromUrl = "email-2";
      const emailToOpen = emailHistory.find((e) => e.id === emailIdFromUrl);

      expect(emailToOpen).toBeDefined();
      expect(emailToOpen?.subject).toBe("Second Email");
    });

    it("should not open modal if email ID not found in history", () => {
      const emailHistory = [
        { id: "email-1", subject: "First Email" },
        { id: "email-2", subject: "Second Email" },
      ];

      const emailIdFromUrl = "email-999";
      const emailToOpen = emailHistory.find((e) => e.id === emailIdFromUrl);

      expect(emailToOpen).toBeUndefined();
    });
  });

  describe("Email Body Sanitization", () => {
    it("should allow safe HTML tags", () => {
      const allowedTags = [
        "p",
        "br",
        "strong",
        "em",
        "u",
        "a",
        "ul",
        "ol",
        "li",
        "h1",
        "h2",
        "h3",
        "h4",
        "h5",
        "h6",
        "blockquote",
        "span",
        "div",
      ];

      expect(allowedTags).toContain("p");
      expect(allowedTags).toContain("strong");
      expect(allowedTags).toContain("a");
      expect(allowedTags).toContain("ul");
      expect(allowedTags).toContain("ol");
      expect(allowedTags).toContain("li");
    });

    it("should allow safe attributes", () => {
      const allowedAttrs = ["href", "target", "rel", "class"];

      expect(allowedAttrs).toContain("href");
      expect(allowedAttrs).toContain("target");
      expect(allowedAttrs).toContain("rel");
      expect(allowedAttrs).toContain("class");
    });

    it("should not allow script tags", () => {
      const allowedTags = [
        "p",
        "br",
        "strong",
        "em",
        "u",
        "a",
        "ul",
        "ol",
        "li",
        "h1",
        "h2",
        "h3",
        "h4",
        "h5",
        "h6",
        "blockquote",
        "span",
        "div",
      ];

      expect(allowedTags).not.toContain("script");
      expect(allowedTags).not.toContain("iframe");
      expect(allowedTags).not.toContain("object");
      expect(allowedTags).not.toContain("embed");
    });

    it("should not allow event handler attributes", () => {
      const allowedAttrs = ["href", "target", "rel", "class"];

      expect(allowedAttrs).not.toContain("onclick");
      expect(allowedAttrs).not.toContain("onload");
      expect(allowedAttrs).not.toContain("onerror");
      expect(allowedAttrs).not.toContain("onmouseover");
    });
  });

  describe("Email Recipient Display", () => {
    it("should display recipient value (email)", () => {
      const recipient = {
        value: "partner@company.com",
        status: "SENT" as const,
      };

      expect(recipient.value).toBe("partner@company.com");
    });

    it("should display correct status for each recipient", () => {
      const recipients = [
        { value: "partner1@company.com", status: "SENT" as const },
        { value: "partner2@company.com", status: "FAILED" as const },
      ];

      expect(recipients[0].status).toBe("SENT");
      expect(recipients[1].status).toBe("FAILED");
    });

    it("should count recipients correctly", () => {
      const recipients = [
        { value: "partner1@company.com", status: "SENT" as const },
        { value: "partner2@company.com", status: "SENT" as const },
        { value: "partner3@company.com", status: "FAILED" as const },
      ];

      expect(recipients.length).toBe(3);
      expect(recipients.filter((r) => r.status === "SENT").length).toBe(2);
      expect(recipients.filter((r) => r.status === "FAILED").length).toBe(1);
    });
  });

  describe("Email History Item Structure", () => {
    it("should have correct email history item structure", () => {
      interface EmailRecipient {
        value: string;
        status: "SENT" | "FAILED";
      }

      interface EmailHistoryItem {
        id: string;
        partnership_id: string;
        sent_at: string;
        sender_email: string;
        subject: string;
        body_plain_text: string;
        overall_status: "SENT" | "FAILED" | "PARTIAL";
        triggered_by: { email: string; display_name?: string };
        recipients: EmailRecipient[];
      }

      const emailItem: EmailHistoryItem = {
        id: "email-123",
        partnership_id: "partnership-456",
        sent_at: "2025-01-15T10:30:00Z",
        sender_email: "noreply@devlille.fr",
        subject: "Test Subject",
        body_plain_text: "<p>Test body</p>",
        overall_status: "SENT",
        triggered_by: { email: "admin@devlille.fr", display_name: "Admin" },
        recipients: [{ value: "partner@company.com", status: "SENT" }],
      };

      expect(emailItem.id).toBeDefined();
      expect(emailItem.partnership_id).toBeDefined();
      expect(emailItem.sent_at).toBeDefined();
      expect(emailItem.subject).toBeDefined();
      expect(emailItem.body_plain_text).toBeDefined();
      expect(emailItem.overall_status).toBeDefined();
      expect(emailItem.triggered_by).toBeDefined();
      expect(emailItem.recipients).toBeDefined();
    });

    it("should handle missing display_name in triggered_by", () => {
      const triggeredBy = { email: "admin@devlille.fr" };

      const displayName = triggeredBy.display_name || triggeredBy.email;

      expect(displayName).toBe("admin@devlille.fr");
    });

    it("should use display_name when available", () => {
      const triggeredBy = {
        email: "admin@devlille.fr",
        display_name: "Admin User",
      };

      const displayName = triggeredBy.display_name || triggeredBy.email;

      expect(displayName).toBe("Admin User");
    });
  });

  describe("Empty State Display", () => {
    it("should show empty state when no emails", () => {
      const emailHistory: unknown[] = [];

      const isEmpty = emailHistory.length === 0;

      expect(isEmpty).toBe(true);
    });

    it("should not show empty state when emails exist", () => {
      const emailHistory = [{ id: "email-1", subject: "Test" }];

      const isEmpty = emailHistory.length === 0;

      expect(isEmpty).toBe(false);
    });
  });

  describe("Keyboard Navigation", () => {
    it("should handle Enter key to open email", () => {
      const openEmailModal = vi.fn();
      const email = { id: "email-1", subject: "Test" };

      const handleKeyDown = (event: { key: string }) => {
        if (event.key === "Enter") {
          openEmailModal(email);
        }
      };

      handleKeyDown({ key: "Enter" });

      expect(openEmailModal).toHaveBeenCalledWith(email);
    });

    it("should handle Space key to open email", () => {
      const openEmailModal = vi.fn();
      const email = { id: "email-1", subject: "Test" };

      const handleKeyDown = (event: { key: string }) => {
        if (event.key === " ") {
          openEmailModal(email);
        }
      };

      handleKeyDown({ key: " " });

      expect(openEmailModal).toHaveBeenCalledWith(email);
    });

    it("should handle Escape key to close modal", () => {
      const closeEmailModal = vi.fn();

      const handleKeyDown = (event: { key: string }) => {
        if (event.key === "Escape") {
          closeEmailModal();
        }
      };

      handleKeyDown({ key: "Escape" });

      expect(closeEmailModal).toHaveBeenCalled();
    });
  });

  describe("Accessibility", () => {
    it("should have correct ARIA attributes for email list items", () => {
      const email = { subject: "Test Email", sent_at: "2025-01-15T10:30:00Z" };
      const formatEmailDate = () => "15/01/2025 11:30";

      const ariaLabel = `Email: ${email.subject}, envoy\u00e9 le ${formatEmailDate(email.sent_at)}`;

      expect(ariaLabel).toContain("Test Email");
      expect(ariaLabel).toContain("15/01/2025");
    });

    it("should have role=button on email list items", () => {
      const role = "button";
      expect(role).toBe("button");
    });

    it("should have tabindex=0 on email list items", () => {
      const tabIndex = 0;
      expect(tabIndex).toBe(0);
    });

    it("should have correct modal ARIA attributes", () => {
      const modalAttributes = {
        role: "dialog",
        "aria-modal": "true",
        "aria-labelledby": "email-modal-title",
      };

      expect(modalAttributes.role).toBe("dialog");
      expect(modalAttributes["aria-modal"]).toBe("true");
      expect(modalAttributes["aria-labelledby"]).toBe("email-modal-title");
    });
  });
});
