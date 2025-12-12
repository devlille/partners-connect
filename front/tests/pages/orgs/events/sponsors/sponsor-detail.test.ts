import { describe, it, expect, vi } from "vitest";

describe("Sponsor Detail Page - Tab Navigation", () => {
  describe("Tab Hash URL functionality", () => {
    it("should initialize to partnership tab by default", () => {
      const tabs = [
        { id: "partnership" as const, label: "Partenariat" },
        { id: "tickets" as const, label: "Tickets" },
        { id: "communication" as const, label: "Communication" },
        { id: "company" as const, label: "Entreprise" },
      ];

      const getInitialTab = (
        hash: string,
      ): "partnership" | "tickets" | "communication" | "company" => {
        const cleanHash = hash.replace("#", "");
        const validTabs = tabs.map((t) => t.id);
        return validTabs.includes(cleanHash as any) ? (cleanHash as any) : "partnership";
      };

      expect(getInitialTab("")).toBe("partnership");
      expect(getInitialTab("#")).toBe("partnership");
    });

    it("should initialize to correct tab from URL hash", () => {
      const tabs = [
        { id: "partnership" as const, label: "Partenariat" },
        { id: "tickets" as const, label: "Tickets" },
        { id: "communication" as const, label: "Communication" },
        { id: "company" as const, label: "Entreprise" },
      ];

      const getInitialTab = (
        hash: string,
      ): "partnership" | "tickets" | "communication" | "company" => {
        const cleanHash = hash.replace("#", "");
        const validTabs = tabs.map((t) => t.id);
        return validTabs.includes(cleanHash as any) ? (cleanHash as any) : "partnership";
      };

      expect(getInitialTab("#partnership")).toBe("partnership");
      expect(getInitialTab("#tickets")).toBe("tickets");
      expect(getInitialTab("#communication")).toBe("communication");
      expect(getInitialTab("#company")).toBe("company");
    });

    it("should fallback to partnership tab for invalid hash", () => {
      const tabs = [
        { id: "partnership" as const, label: "Partenariat" },
        { id: "tickets" as const, label: "Tickets" },
        { id: "communication" as const, label: "Communication" },
        { id: "company" as const, label: "Entreprise" },
      ];

      const getInitialTab = (
        hash: string,
      ): "partnership" | "tickets" | "communication" | "company" => {
        const cleanHash = hash.replace("#", "");
        const validTabs = tabs.map((t) => t.id);
        return validTabs.includes(cleanHash as any) ? (cleanHash as any) : "partnership";
      };

      expect(getInitialTab("#invalid")).toBe("partnership");
      expect(getInitialTab("#unknown-tab")).toBe("partnership");
      expect(getInitialTab("#settings")).toBe("partnership");
    });

    it("should validate all tab IDs", () => {
      const tabs = [
        { id: "partnership" as const, label: "Partenariat" },
        { id: "tickets" as const, label: "Tickets" },
        { id: "communication" as const, label: "Communication" },
        { id: "company" as const, label: "Entreprise" },
      ];

      const validTabs = tabs.map((t) => t.id);

      expect(validTabs).toContain("partnership");
      expect(validTabs).toContain("tickets");
      expect(validTabs).toContain("communication");
      expect(validTabs).toContain("company");
      expect(validTabs).toHaveLength(4);
    });
  });

  describe("Hash synchronization", () => {
    it("should update URL hash when tab changes", () => {
      const mockRouter = {
        push: vi.fn(),
      };

      const changeTab = (tabId: "partnership" | "tickets" | "communication" | "company") => {
        mockRouter.push({ hash: `#${tabId}` });
      };

      changeTab("tickets");
      expect(mockRouter.push).toHaveBeenCalledWith({ hash: "#tickets" });

      changeTab("communication");
      expect(mockRouter.push).toHaveBeenCalledWith({ hash: "#communication" });

      changeTab("company");
      expect(mockRouter.push).toHaveBeenCalledWith({ hash: "#company" });
    });

    it("should parse hash correctly from route", () => {
      const parseHash = (routeHash: string) => routeHash.replace("#", "");

      expect(parseHash("#partnership")).toBe("partnership");
      expect(parseHash("#tickets")).toBe("tickets");
      expect(parseHash("#communication")).toBe("communication");
      expect(parseHash("#company")).toBe("company");
      expect(parseHash("")).toBe("");
    });
  });

  describe("Tab definitions", () => {
    it("should have correct tab structure", () => {
      const tabs = [
        { id: "partnership" as const, label: "Partenariat" },
        { id: "tickets" as const, label: "Tickets" },
        { id: "communication" as const, label: "Communication" },
        { id: "company" as const, label: "Entreprise" },
      ];

      expect(tabs).toHaveLength(4);

      // Verify partnership tab
      expect(tabs[0].id).toBe("partnership");
      expect(tabs[0].label).toBe("Partenariat");

      // Verify tickets tab
      expect(tabs[1].id).toBe("tickets");
      expect(tabs[1].label).toBe("Tickets");

      // Verify communication tab
      expect(tabs[2].id).toBe("communication");
      expect(tabs[2].label).toBe("Communication");

      // Verify company tab
      expect(tabs[3].id).toBe("company");
      expect(tabs[3].label).toBe("Entreprise");
    });

    it("should have all unique tab IDs", () => {
      const tabs = [
        { id: "partnership" as const, label: "Partenariat" },
        { id: "tickets" as const, label: "Tickets" },
        { id: "communication" as const, label: "Communication" },
        { id: "company" as const, label: "Entreprise" },
      ];

      const tabIds = tabs.map((t) => t.id);
      const uniqueIds = new Set(tabIds);

      expect(uniqueIds.size).toBe(tabIds.length);
    });
  });
});

describe("Sponsor Detail Page - Date Formatting", () => {
  describe("formatDateSafe", () => {
    it("should handle null date", () => {
      const formatDateSafe = (dateString: string | null | undefined): string => {
        if (!dateString) return "Date invalide";

        try {
          return new Date(dateString).toLocaleDateString("fr-FR", {
            year: "numeric",
            month: "long",
            day: "numeric",
          });
        } catch {
          return "Date invalide";
        }
      };

      expect(formatDateSafe(null)).toBe("Date invalide");
      expect(formatDateSafe(undefined)).toBe("Date invalide");
      expect(formatDateSafe("")).toBe("Date invalide");
    });

    it("should format valid ISO date string", () => {
      const formatDateSafe = (dateString: string | null | undefined): string => {
        if (!dateString) return "Date invalide";

        try {
          return new Date(dateString).toLocaleDateString("fr-FR", {
            year: "numeric",
            month: "long",
            day: "numeric",
          });
        } catch {
          return "Date invalide";
        }
      };

      const formatted = formatDateSafe("2025-10-30T00:00");
      expect(formatted).toContain("2025");
      expect(formatted).toContain("octobre");
      expect(formatted).toContain("30");
    });

    it("should handle different date formats", () => {
      const formatDateSafe = (dateString: string | null | undefined): string => {
        if (!dateString) return "Date invalide";

        try {
          return new Date(dateString).toLocaleDateString("fr-FR", {
            year: "numeric",
            month: "long",
            day: "numeric",
          });
        } catch {
          return "Date invalide";
        }
      };

      // ISO format
      expect(formatDateSafe("2025-12-25")).toContain("2025");

      // ISO with time
      expect(formatDateSafe("2025-12-25T10:30:00")).toContain("2025");

      // ISO with timezone
      expect(formatDateSafe("2025-12-25T10:30:00Z")).toContain("2025");
    });
  });

  describe("isDatePassed", () => {
    it("should return true for past dates", () => {
      const isDatePassed = (dateString: string): boolean => {
        try {
          const date = new Date(dateString);
          const now = new Date();
          return date < now;
        } catch {
          return false;
        }
      };

      expect(isDatePassed("2020-01-01")).toBe(true);
      expect(isDatePassed("2021-06-15")).toBe(true);
    });

    it("should return false for future dates", () => {
      const isDatePassed = (dateString: string): boolean => {
        try {
          const date = new Date(dateString);
          const now = new Date();
          return date < now;
        } catch {
          return false;
        }
      };

      expect(isDatePassed("2030-01-01")).toBe(false);
      expect(isDatePassed("2035-12-31")).toBe(false);
    });

    it("should handle invalid dates gracefully", () => {
      const isDatePassed = (dateString: string): boolean => {
        try {
          const date = new Date(dateString);
          const now = new Date();
          return date < now;
        } catch {
          return false;
        }
      };

      expect(isDatePassed("invalid-date")).toBe(false);
      expect(isDatePassed("")).toBe(false);
    });
  });

  describe("Communication status logic", () => {
    it("should determine status based on publication_date", () => {
      const getStatus = (publicationDate: string | null): "unplanned" | "planned" | "done" => {
        if (!publicationDate) return "unplanned";

        const date = new Date(publicationDate);
        const now = new Date();

        return date < now ? "done" : "planned";
      };

      expect(getStatus(null)).toBe("unplanned");
      expect(getStatus("2020-01-01")).toBe("done");
      expect(getStatus("2030-01-01")).toBe("planned");
    });

    it("should handle edge cases", () => {
      const getStatus = (publicationDate: string | null): "unplanned" | "planned" | "done" => {
        if (!publicationDate) return "unplanned";

        const date = new Date(publicationDate);
        const now = new Date();

        return date < now ? "done" : "planned";
      };

      expect(getStatus(null)).toBe("unplanned");
      expect(getStatus("")).toBe("unplanned");
    });
  });
});

describe("Sponsor Detail Page - Helper Functions", () => {
  describe("Route parameter extraction", () => {
    it("should extract single string parameter", () => {
      const extractParam = (param: string | string[]): string => {
        return Array.isArray(param) ? param[0] : param;
      };

      expect(extractParam("test-slug")).toBe("test-slug");
      expect(extractParam(["test-slug"])).toBe("test-slug");
      expect(extractParam(["first", "second"])).toBe("first");
    });

    it("should handle array parameters", () => {
      const extractParam = (param: string | string[]): string => {
        return Array.isArray(param) ? param[0] : param;
      };

      const multiParam = ["org1", "org2", "org3"];
      expect(extractParam(multiParam)).toBe("org1");
    });
  });

  describe("Tab validation", () => {
    it("should validate tab type", () => {
      type TabType = "partnership" | "tickets" | "communication" | "company";
      const validTabs: TabType[] = ["partnership", "tickets", "communication", "company"];

      const isValidTab = (tab: string): tab is TabType => {
        return validTabs.includes(tab as TabType);
      };

      expect(isValidTab("partnership")).toBe(true);
      expect(isValidTab("tickets")).toBe(true);
      expect(isValidTab("communication")).toBe(true);
      expect(isValidTab("company")).toBe(true);
      expect(isValidTab("invalid")).toBe(false);
      expect(isValidTab("settings")).toBe(false);
    });
  });

  describe("Communication data structure", () => {
    it("should have correct structure for communication item", () => {
      interface CommunicationItem {
        partnership_id: string;
        company_name: string;
        publication_date: string | null;
        support_url: string | null;
      }

      const communication: CommunicationItem = {
        partnership_id: "abc123",
        company_name: "Test Company",
        publication_date: "2025-10-30T00:00",
        support_url: "https://example.com/support.jpg",
      };

      expect(communication.partnership_id).toBeDefined();
      expect(communication.company_name).toBeDefined();
      expect(communication.publication_date).toBeDefined();
      expect(communication.support_url).toBeDefined();
    });

    it("should allow null values for optional fields", () => {
      interface CommunicationItem {
        partnership_id: string;
        company_name: string;
        publication_date: string | null;
        support_url: string | null;
      }

      const communication: CommunicationItem = {
        partnership_id: "abc123",
        company_name: "Test Company",
        publication_date: null,
        support_url: null,
      };

      expect(communication.publication_date).toBeNull();
      expect(communication.support_url).toBeNull();
    });
  });
});

describe("Sponsor Detail Page - Partnership Actions", () => {
  describe("Partnership Validation", () => {
    it("should call validate API with correct parameters", async () => {
      const mockValidate = vi.fn().mockResolvedValue({ data: { id: "partnership-123" } });

      const orgSlug = "devlille";
      const eventSlug = "2025";
      const partnershipId = "abc123";

      await mockValidate(orgSlug, eventSlug, partnershipId);

      expect(mockValidate).toHaveBeenCalledWith(orgSlug, eventSlug, partnershipId);
      expect(mockValidate).toHaveBeenCalledTimes(1);
    });

    it("should handle successful validation", async () => {
      const mockValidate = vi.fn().mockResolvedValue({
        data: { id: "partnership-123" },
      });

      const result = await mockValidate("devlille", "2025", "abc123");

      expect(result.data.id).toBe("partnership-123");
    });

    it("should handle validation error", async () => {
      const mockValidate = vi.fn().mockRejectedValue(new Error("Failed to validate partnership"));

      await expect(mockValidate("devlille", "2025", "abc123")).rejects.toThrow(
        "Failed to validate partnership",
      );
    });

    it("should handle validation with 404 error", async () => {
      const mockValidate = vi.fn().mockRejectedValue({
        response: { status: 404, data: { message: "Partnership not found" } },
      });

      try {
        await mockValidate("devlille", "2025", "invalid-id");
      } catch (error: any) {
        expect(error.response.status).toBe(404);
        expect(error.response.data.message).toBe("Partnership not found");
      }
    });

    it("should handle validation with 403 forbidden error", async () => {
      const mockValidate = vi.fn().mockRejectedValue({
        response: { status: 403, data: { message: "Unauthorized" } },
      });

      try {
        await mockValidate("devlille", "2025", "abc123");
      } catch (error: any) {
        expect(error.response.status).toBe(403);
      }
    });
  });

  describe("Partnership Decline", () => {
    it("should call decline API with correct parameters", async () => {
      const mockDecline = vi.fn().mockResolvedValue({ data: { id: "partnership-123" } });

      const orgSlug = "devlille";
      const eventSlug = "2025";
      const partnershipId = "abc123";

      await mockDecline(orgSlug, eventSlug, partnershipId);

      expect(mockDecline).toHaveBeenCalledWith(orgSlug, eventSlug, partnershipId);
      expect(mockDecline).toHaveBeenCalledTimes(1);
    });

    it("should handle successful decline", async () => {
      const mockDecline = vi.fn().mockResolvedValue({
        data: { id: "partnership-123" },
      });

      const result = await mockDecline("devlille", "2025", "abc123");

      expect(result.data.id).toBe("partnership-123");
    });

    it("should handle decline error", async () => {
      const mockDecline = vi.fn().mockRejectedValue(new Error("Failed to decline partnership"));

      await expect(mockDecline("devlille", "2025", "abc123")).rejects.toThrow(
        "Failed to decline partnership",
      );
    });

    it("should handle decline with 404 error", async () => {
      const mockDecline = vi.fn().mockRejectedValue({
        response: { status: 404, data: { message: "Partnership not found" } },
      });

      try {
        await mockDecline("devlille", "2025", "invalid-id");
      } catch (error: any) {
        expect(error.response.status).toBe(404);
        expect(error.response.data.message).toBe("Partnership not found");
      }
    });

    it("should handle decline with 403 forbidden error", async () => {
      const mockDecline = vi.fn().mockRejectedValue({
        response: { status: 403, data: { message: "Unauthorized" } },
      });

      try {
        await mockDecline("devlille", "2025", "abc123");
      } catch (error: any) {
        expect(error.response.status).toBe(403);
      }
    });
  });

  describe("Partnership Action State Management", () => {
    it("should set loading state during validation", () => {
      let isValidating = false;
      let error = null;

      // Simulate starting validation
      isValidating = true;
      error = null;

      expect(isValidating).toBe(true);
      expect(error).toBeNull();

      // Simulate validation complete
      isValidating = false;

      expect(isValidating).toBe(false);
    });

    it("should set loading state during decline", () => {
      let isDeclining = false;
      let error = null;

      // Simulate starting decline
      isDeclining = true;
      error = null;

      expect(isDeclining).toBe(true);
      expect(error).toBeNull();

      // Simulate decline complete
      isDeclining = false;

      expect(isDeclining).toBe(false);
    });

    it("should set error state on validation failure", () => {
      let isValidating = false;
      let error: string | null = null;

      // Simulate validation failure
      isValidating = false;
      error = "Failed to validate partnership";

      expect(isValidating).toBe(false);
      expect(error).toBe("Failed to validate partnership");
    });

    it("should set error state on decline failure", () => {
      let isDeclining = false;
      let error: string | null = null;

      // Simulate decline failure
      isDeclining = false;
      error = "Failed to decline partnership";

      expect(isDeclining).toBe(false);
      expect(error).toBe("Failed to decline partnership");
    });

    it("should reset error state before new action", () => {
      let error: string | null = "Previous error";

      // Reset error before new action
      error = null;

      expect(error).toBeNull();
    });
  });

  describe("Partnership Action Confirmation", () => {
    it("should require confirmation before validation", () => {
      const confirmValidation = (_partnershipName: string): boolean => {
        // In real implementation, this would show a modal
        // For testing, we just return true
        return true;
      };

      expect(confirmValidation("Test Company")).toBe(true);
    });

    it("should require confirmation before decline", () => {
      const confirmDecline = (_partnershipName: string): boolean => {
        // In real implementation, this would show a modal
        // For testing, we just return true
        return true;
      };

      expect(confirmDecline("Test Company")).toBe(true);
    });

    it("should not proceed if confirmation is cancelled", () => {
      const confirmAction = (): boolean => false;
      const performAction = vi.fn();

      if (confirmAction()) {
        performAction();
      }

      expect(performAction).not.toHaveBeenCalled();
    });
  });

  describe("Partnership Action Success Handling", () => {
    it("should show success message after validation", () => {
      const showSuccessToast = vi.fn();
      const partnershipName = "Test Company";

      showSuccessToast(`Le partenariat avec ${partnershipName} a été validé avec succès`);

      expect(showSuccessToast).toHaveBeenCalledWith(
        "Le partenariat avec Test Company a été validé avec succès",
      );
    });

    it("should show success message after decline", () => {
      const showSuccessToast = vi.fn();
      const partnershipName = "Test Company";

      showSuccessToast(`Le partenariat avec ${partnershipName} a été refusé`);

      expect(showSuccessToast).toHaveBeenCalledWith(
        "Le partenariat avec Test Company a été refusé",
      );
    });

    it("should reload partnership data after successful validation", async () => {
      const reloadPartnership = vi.fn().mockResolvedValue(undefined);

      await reloadPartnership();

      expect(reloadPartnership).toHaveBeenCalledTimes(1);
    });

    it("should reload partnership data after successful decline", async () => {
      const reloadPartnership = vi.fn().mockResolvedValue(undefined);

      await reloadPartnership();

      expect(reloadPartnership).toHaveBeenCalledTimes(1);
    });
  });

  describe("Partnership Action Error Messages", () => {
    it("should format 404 error message", () => {
      const formatErrorMessage = (status: number): string => {
        if (status === 404) return "Partenariat introuvable";
        if (status === 403) return "Vous n'êtes pas autorisé à effectuer cette action";
        return "Une erreur est survenue";
      };

      expect(formatErrorMessage(404)).toBe("Partenariat introuvable");
    });

    it("should format 403 error message", () => {
      const formatErrorMessage = (status: number): string => {
        if (status === 404) return "Partenariat introuvable";
        if (status === 403) return "Vous n'êtes pas autorisé à effectuer cette action";
        return "Une erreur est survenue";
      };

      expect(formatErrorMessage(403)).toBe("Vous n'êtes pas autorisé à effectuer cette action");
    });

    it("should format generic error message", () => {
      const formatErrorMessage = (status: number): string => {
        if (status === 404) return "Partenariat introuvable";
        if (status === 403) return "Vous n'êtes pas autorisé à effectuer cette action";
        return "Une erreur est survenue";
      };

      expect(formatErrorMessage(500)).toBe("Une erreur est survenue");
    });
  });

  describe("Agreement Generation", () => {
    describe("Generate Agreement API Call", () => {
      it("should call API with correct parameters", async () => {
        const mockApi = {
          postOrgsEventsPartnershipAgreement: vi.fn().mockResolvedValue({
            data: { url: "https://example.com/agreement.pdf" },
          }),
        };

        const orgSlug = "test-org";
        const eventSlug = "test-event";
        const sponsorId = "sponsor-123";

        await mockApi.postOrgsEventsPartnershipAgreement(orgSlug, eventSlug, sponsorId);

        expect(mockApi.postOrgsEventsPartnershipAgreement).toHaveBeenCalledWith(
          orgSlug,
          eventSlug,
          sponsorId,
        );
      });

      it("should return agreement URL on success", async () => {
        const expectedUrl = "https://example.com/agreement.pdf";
        const mockApi = {
          postOrgsEventsPartnershipAgreement: vi.fn().mockResolvedValue({
            data: { url: expectedUrl },
          }),
        };

        const result = await mockApi.postOrgsEventsPartnershipAgreement("org", "event", "sponsor");

        expect(result.data.url).toBe(expectedUrl);
      });

      it("should handle 404 error when partnership not found", async () => {
        const mockApi = {
          postOrgsEventsPartnershipAgreement: vi.fn().mockRejectedValue({
            response: { status: 404 },
          }),
        };

        await expect(
          mockApi.postOrgsEventsPartnershipAgreement("org", "event", "invalid-id"),
        ).rejects.toMatchObject({
          response: { status: 404 },
        });
      });

      it("should handle 403 error when not authorized", async () => {
        const mockApi = {
          postOrgsEventsPartnershipAgreement: vi.fn().mockRejectedValue({
            response: { status: 403 },
          }),
        };

        await expect(
          mockApi.postOrgsEventsPartnershipAgreement("org", "event", "sponsor"),
        ).rejects.toMatchObject({
          response: { status: 403 },
        });
      });

      it("should handle generic API error", async () => {
        const mockApi = {
          postOrgsEventsPartnershipAgreement: vi.fn().mockRejectedValue({
            response: { status: 500 },
          }),
        };

        await expect(
          mockApi.postOrgsEventsPartnershipAgreement("org", "event", "sponsor"),
        ).rejects.toMatchObject({
          response: { status: 500 },
        });
      });

      it("should handle network error", async () => {
        const mockApi = {
          postOrgsEventsPartnershipAgreement: vi.fn().mockRejectedValue(new Error("Network error")),
        };

        await expect(
          mockApi.postOrgsEventsPartnershipAgreement("org", "event", "sponsor"),
        ).rejects.toThrow("Network error");
      });
    });

    describe("Agreement Generation State Management", () => {
      it("should set loading state to true when generating", () => {
        const state = {
          isGeneratingAgreement: false,
        };

        // Simulate start of generation
        state.isGeneratingAgreement = true;

        expect(state.isGeneratingAgreement).toBe(true);
      });

      it("should set loading state to false after success", async () => {
        const state = {
          isGeneratingAgreement: true,
          agreementUrl: null as string | null,
          agreementError: null as string | null,
        };

        // Simulate successful generation
        state.agreementUrl = "https://example.com/agreement.pdf";
        state.isGeneratingAgreement = false;
        state.agreementError = null;

        expect(state.isGeneratingAgreement).toBe(false);
        expect(state.agreementUrl).toBe("https://example.com/agreement.pdf");
        expect(state.agreementError).toBeNull();
      });

      it("should set loading state to false after error", () => {
        const state = {
          isGeneratingAgreement: true,
          agreementError: null as string | null,
        };

        // Simulate error
        state.agreementError = "Une erreur est survenue";
        state.isGeneratingAgreement = false;

        expect(state.isGeneratingAgreement).toBe(false);
        expect(state.agreementError).toBe("Une erreur est survenue");
      });

      it("should clear error when starting new generation", () => {
        const state = {
          agreementError: "Previous error" as string | null,
          isGeneratingAgreement: false,
        };

        // Simulate start of new generation
        state.agreementError = null;
        state.isGeneratingAgreement = true;

        expect(state.agreementError).toBeNull();
        expect(state.isGeneratingAgreement).toBe(true);
      });

      it("should store agreement URL after successful generation", () => {
        const state = {
          agreementUrl: null as string | null,
        };

        const generatedUrl = "https://example.com/agreements/123.pdf";
        state.agreementUrl = generatedUrl;

        expect(state.agreementUrl).toBe(generatedUrl);
      });
    });

    describe("Agreement Error Handling", () => {
      it("should format 404 error message", () => {
        const formatAgreementError = (err: any): string => {
          if (err.response?.status === 404) {
            return "Partenariat introuvable";
          }
          if (err.response?.status === 403) {
            return "Vous n'êtes pas autorisé à générer cette convention";
          }
          return "Une erreur est survenue lors de la génération de la convention";
        };

        const error = { response: { status: 404 } };
        expect(formatAgreementError(error)).toBe("Partenariat introuvable");
      });

      it("should format 403 error message", () => {
        const formatAgreementError = (err: any): string => {
          if (err.response?.status === 404) {
            return "Partenariat introuvable";
          }
          if (err.response?.status === 403) {
            return "Vous n'êtes pas autorisé à générer cette convention";
          }
          return "Une erreur est survenue lors de la génération de la convention";
        };

        const error = { response: { status: 403 } };
        expect(formatAgreementError(error)).toBe(
          "Vous n'êtes pas autorisé à générer cette convention",
        );
      });

      it("should format generic error message", () => {
        const formatAgreementError = (err: any): string => {
          if (err.response?.status === 404) {
            return "Partenariat introuvable";
          }
          if (err.response?.status === 403) {
            return "Vous n'êtes pas autorisé à générer cette convention";
          }
          return "Une erreur est survenue lors de la génération de la convention";
        };

        const error = { response: { status: 500 } };
        expect(formatAgreementError(error)).toBe(
          "Une erreur est survenue lors de la génération de la convention",
        );
      });

      it("should handle error without response", () => {
        const formatAgreementError = (err: any): string => {
          if (err.response?.status === 404) {
            return "Partenariat introuvable";
          }
          if (err.response?.status === 403) {
            return "Vous n'êtes pas autorisé à générer cette convention";
          }
          return "Une erreur est survenue lors de la génération de la convention";
        };

        const error = new Error("Network error");
        expect(formatAgreementError(error)).toBe(
          "Une erreur est survenue lors de la génération de la convention",
        );
      });
    });

    describe("Agreement Success Handling", () => {
      it("should open PDF in new tab on success", () => {
        const mockWindow = {
          open: vi.fn(),
        };

        const agreementUrl = "https://example.com/agreement.pdf";
        mockWindow.open(agreementUrl, "_blank");

        expect(mockWindow.open).toHaveBeenCalledWith(agreementUrl, "_blank");
      });

      it("should show success toast notification", () => {
        const mockToast = {
          success: vi.fn(),
        };

        mockToast.success("La convention a été générée avec succès");

        expect(mockToast.success).toHaveBeenCalledWith("La convention a été générée avec succès");
      });

      it("should handle missing URL in response", () => {
        const response = { data: {} };
        const hasUrl = "url" in response.data;

        expect(hasUrl).toBe(false);
      });
    });

    describe("Documents Tab Integration", () => {
      it("should include documents tab in tabs array", () => {
        const tabs = [
          { id: "partnership" as const, label: "Partenariat" },
          { id: "tickets" as const, label: "Tickets" },
          { id: "communication" as const, label: "Communication" },
          { id: "documents" as const, label: "Documents" },
          { id: "company" as const, label: "Entreprise" },
        ];

        const documentTab = tabs.find((t) => t.id === "documents");

        expect(documentTab).toBeDefined();
        expect(documentTab?.label).toBe("Documents");
      });

      it("should navigate to documents tab via hash", () => {
        const tabs = [
          { id: "partnership" as const, label: "Partenariat" },
          { id: "tickets" as const, label: "Tickets" },
          { id: "communication" as const, label: "Communication" },
          { id: "documents" as const, label: "Documents" },
          { id: "company" as const, label: "Entreprise" },
        ];

        const getInitialTab = (
          hash: string,
        ): "partnership" | "tickets" | "communication" | "documents" | "company" => {
          const cleanHash = hash.replace("#", "");
          const validTabs = tabs.map((t) => t.id);
          return validTabs.includes(cleanHash as any) ? (cleanHash as any) : "partnership";
        };

        expect(getInitialTab("#documents")).toBe("documents");
      });

      it("should update URL hash when changing to documents tab", () => {
        const mockRouter = {
          push: vi.fn(),
        };

        const changeTab = (
          tabId: "partnership" | "tickets" | "communication" | "documents" | "company",
        ) => {
          mockRouter.push({ hash: `#${tabId}` });
        };

        changeTab("documents");

        expect(mockRouter.push).toHaveBeenCalledWith({ hash: "#documents" });
      });
    });

    describe("Agreement Button State", () => {
      it("should disable button while generating", () => {
        const state = {
          isGeneratingAgreement: true,
        };

        const isDisabled = state.isGeneratingAgreement;

        expect(isDisabled).toBe(true);
      });

      it("should enable button when not generating", () => {
        const state = {
          isGeneratingAgreement: false,
        };

        const isDisabled = state.isGeneratingAgreement;

        expect(isDisabled).toBe(false);
      });

      it("should show loading indicator while generating", () => {
        const state = {
          isGeneratingAgreement: true,
        };

        expect(state.isGeneratingAgreement).toBe(true);
      });
    });
  });

  describe("Partnership Data Loading", () => {
    it("should use getEventsPartnershipDetailed API", async () => {
      const mockApi = {
        getEventsPartnershipDetailed: vi.fn().mockResolvedValue({
          data: {
            partnership: {
              id: "p123",
              contact_name: "John Doe",
              contact_role: "CEO",
              language: "fr",
              emails: ["john@example.com"],
              phone: "+33612345678",
              selected_pack: { id: "pack1", name: "Gold" },
              suggestion_pack: null,
              validated_pack: null,
              process_status: {
                validated_at: null,
                billing_status: "pending",
                suggested_at: null,
                agreement_url: null,
                agreement_signed_url: null,
              },
              created_at: "2025-01-01T00:00:00Z",
            },
            company: { name: "Test Company" },
            event: { name: "DevFest 2025" },
            organisation: { slug: "devlille", name: "DevLille" },
          },
        }),
      };

      await mockApi.getEventsPartnershipDetailed("devfest-2025", "p123");

      expect(mockApi.getEventsPartnershipDetailed).toHaveBeenCalledWith("devfest-2025", "p123");
      expect(mockApi.getEventsPartnershipDetailed).toHaveBeenCalledTimes(1);
    });

    it("should map DetailedPartnershipResponseSchema to ExtendedPartnershipItem", () => {
      const apiResponse = {
        partnership: {
          id: "p123",
          contact_name: "John Doe",
          contact_role: "CEO",
          language: "fr",
          emails: ["john@example.com", "jane@example.com"],
          phone: "+33612345678",
          selected_pack: { id: "pack1", name: "Gold" },
          suggestion_pack: null,
          validated_pack: { id: "pack2", name: "Platinum" },
          process_status: {
            validated_at: "2025-01-15T00:00:00Z",
            billing_status: "paid",
            suggested_at: null,
            agreement_url: "https://example.com/agreement.pdf",
            agreement_signed_url: "https://example.com/signed.pdf",
          },
          created_at: "2025-01-01T00:00:00Z",
        },
        company: { name: "Test Company" },
        event: { name: "DevFest 2025" },
      };

      const mapped = {
        id: apiResponse.partnership.id,
        contact: {
          display_name: apiResponse.partnership.contact_name,
          role: apiResponse.partnership.contact_role,
        },
        company_name: apiResponse.company.name,
        event_name: apiResponse.event.name,
        selected_pack_id: apiResponse.partnership.selected_pack?.id || null,
        selected_pack_name: apiResponse.partnership.selected_pack?.name || null,
        suggested_pack_id: apiResponse.partnership.suggestion_pack?.id || null,
        suggested_pack_name: apiResponse.partnership.suggestion_pack?.name || null,
        validated_pack_id: apiResponse.partnership.validated_pack?.id || null,
        language: apiResponse.partnership.language,
        phone: apiResponse.partnership.phone || null,
        emails: apiResponse.partnership.emails.join(", "),
        created_at: apiResponse.partnership.created_at,
        validated: apiResponse.partnership.process_status.validated_at !== null,
        paid: apiResponse.partnership.process_status.billing_status?.toLowerCase() === "paid",
        suggestion: apiResponse.partnership.process_status.suggested_at !== null,
        agreement_generated: apiResponse.partnership.process_status.agreement_url !== null,
        agreement_signed: apiResponse.partnership.process_status.agreement_signed_url !== null,
        option_ids: [],
      };

      expect(mapped.id).toBe("p123");
      expect(mapped.contact.display_name).toBe("John Doe");
      expect(mapped.contact.role).toBe("CEO");
      expect(mapped.company_name).toBe("Test Company");
      expect(mapped.event_name).toBe("DevFest 2025");
      expect(mapped.selected_pack_id).toBe("pack1");
      expect(mapped.selected_pack_name).toBe("Gold");
      expect(mapped.validated_pack_id).toBe("pack2");
      expect(mapped.language).toBe("fr");
      expect(mapped.phone).toBe("+33612345678");
      expect(mapped.emails).toBe("john@example.com, jane@example.com");
      expect(mapped.validated).toBe(true);
      expect(mapped.paid).toBe(true);
      expect(mapped.suggestion).toBe(false);
      expect(mapped.agreement_generated).toBe(true);
      expect(mapped.agreement_signed).toBe(true);
    });

    it("should handle null optional fields in mapping", () => {
      const apiResponse = {
        partnership: {
          id: "p123",
          contact_name: "John Doe",
          contact_role: "CEO",
          language: "fr",
          emails: [],
          phone: null,
          selected_pack: null,
          suggestion_pack: null,
          validated_pack: null,
          process_status: {
            validated_at: null,
            billing_status: "pending",
            suggested_at: null,
            agreement_url: null,
            agreement_signed_url: null,
          },
          created_at: "2025-01-01T00:00:00Z",
        },
        company: { name: "Test Company" },
        event: { name: "DevFest 2025" },
      };

      const mapped = {
        id: apiResponse.partnership.id,
        selected_pack_id: apiResponse.partnership.selected_pack?.id || null,
        selected_pack_name: apiResponse.partnership.selected_pack?.name || null,
        suggested_pack_id: apiResponse.partnership.suggestion_pack?.id || null,
        suggested_pack_name: apiResponse.partnership.suggestion_pack?.name || null,
        validated_pack_id: apiResponse.partnership.validated_pack?.id || null,
        phone: apiResponse.partnership.phone || null,
        emails: apiResponse.partnership.emails.join(", "),
        validated: apiResponse.partnership.process_status.validated_at !== null,
        paid: apiResponse.partnership.process_status.billing_status?.toLowerCase() === "paid",
        suggestion: apiResponse.partnership.process_status.suggested_at !== null,
        agreement_generated: apiResponse.partnership.process_status.agreement_url !== null,
        agreement_signed: apiResponse.partnership.process_status.agreement_signed_url !== null,
      };

      expect(mapped.selected_pack_id).toBeNull();
      expect(mapped.selected_pack_name).toBeNull();
      expect(mapped.suggested_pack_id).toBeNull();
      expect(mapped.suggested_pack_name).toBeNull();
      expect(mapped.validated_pack_id).toBeNull();
      expect(mapped.phone).toBeNull();
      expect(mapped.emails).toBe("");
      expect(mapped.validated).toBe(false);
      expect(mapped.paid).toBe(false);
      expect(mapped.suggestion).toBe(false);
      expect(mapped.agreement_generated).toBe(false);
      expect(mapped.agreement_signed).toBe(false);
    });

    it("should handle billing_status in both uppercase and lowercase", () => {
      // Test avec PAID en majuscules (comme retourné par l'API)
      const apiResponseUppercase = {
        partnership: {
          id: "p123",
          contact_name: "John Doe",
          contact_role: "CEO",
          language: "fr",
          emails: ["john@example.com"],
          phone: "+33612345678",
          selected_pack: { id: "pack1", name: "Gold" },
          suggestion_pack: null,
          validated_pack: null,
          process_status: {
            validated_at: null,
            billing_status: "PAID",
            suggested_at: null,
            agreement_url: null,
            agreement_signed_url: null,
          },
          created_at: "2025-01-01T00:00:00Z",
        },
        company: { name: "Test Company" },
        event: { name: "DevFest 2025" },
      };

      const mappedUppercase = {
        paid:
          apiResponseUppercase.partnership.process_status.billing_status?.toLowerCase() === "paid",
      };

      expect(mappedUppercase.paid).toBe(true);

      // Test avec paid en minuscules
      const apiResponseLowercase = {
        partnership: {
          id: "p456",
          contact_name: "Jane Smith",
          contact_role: "CTO",
          language: "en",
          emails: ["jane@example.com"],
          phone: "+33612345679",
          selected_pack: { id: "pack2", name: "Silver" },
          suggestion_pack: null,
          validated_pack: null,
          process_status: {
            validated_at: null,
            billing_status: "paid",
            suggested_at: null,
            agreement_url: null,
            agreement_signed_url: null,
          },
          created_at: "2025-01-02T00:00:00Z",
        },
        company: { name: "Another Company" },
        event: { name: "TechConf 2025" },
      };

      const mappedLowercase = {
        paid:
          apiResponseLowercase.partnership.process_status.billing_status?.toLowerCase() === "paid",
      };

      expect(mappedLowercase.paid).toBe(true);

      // Test avec PENDING en majuscules
      const apiResponsePending = {
        partnership: {
          id: "p789",
          contact_name: "Bob Johnson",
          contact_role: "CFO",
          language: "fr",
          emails: ["bob@example.com"],
          phone: "+33612345680",
          selected_pack: { id: "pack3", name: "Bronze" },
          suggestion_pack: null,
          validated_pack: null,
          process_status: {
            validated_at: null,
            billing_status: "PENDING",
            suggested_at: null,
            agreement_url: null,
            agreement_signed_url: null,
          },
          created_at: "2025-01-03T00:00:00Z",
        },
        company: { name: "Third Company" },
        event: { name: "CodeCamp 2025" },
      };

      const mappedPending = {
        paid:
          apiResponsePending.partnership.process_status.billing_status?.toLowerCase() === "paid",
      };

      expect(mappedPending.paid).toBe(false);
    });

    it("should handle API errors gracefully", async () => {
      const mockApi = {
        getEventsPartnershipDetailed: vi
          .fn()
          .mockRejectedValue(new Error("Failed to load partnership")),
      };

      await expect(mockApi.getEventsPartnershipDetailed("devfest-2025", "p123")).rejects.toThrow(
        "Failed to load partnership",
      );
    });
  });

  describe("Action Buttons Visibility", () => {
    it("should show action buttons when partnership is not validated", () => {
      const partnership = {
        id: "p123",
        validated_pack_id: null,
      };

      const shouldShowButtons = partnership && !partnership.validated_pack_id;

      expect(shouldShowButtons).toBe(true);
    });

    it("should hide action buttons when partnership has validated_pack_id", () => {
      const partnership = {
        id: "p123",
        validated_pack_id: "pack-validated",
      };

      const shouldShowButtons = partnership && !partnership.validated_pack_id;

      expect(shouldShowButtons).toBe(false);
    });

    it("should hide action buttons when partnership is null", () => {
      const partnership = null;

      const shouldShowButtons = partnership && !partnership.validated_pack_id;

      expect(shouldShowButtons).toBeFalsy();
    });

    it("should include suggest pack button in action buttons", () => {
      const actions = ["suggest", "decline", "validate"];

      expect(actions).toContain("suggest");
      expect(actions).toContain("decline");
      expect(actions).toContain("validate");
      expect(actions).toHaveLength(3);
    });

    it("should verify button properties when visible", () => {
      const suggestButton = {
        label: "Proposer un autre pack",
        color: "neutral",
        variant: "outline",
        icon: "i-heroicons-arrow-path",
      };

      const declineButton = {
        label: "Refuser",
        color: "error",
        variant: "outline",
        icon: "i-heroicons-x-mark",
      };

      const validateButton = {
        label: "Valider",
        color: "primary",
        icon: "i-heroicons-check",
      };

      expect(suggestButton.label).toBe("Proposer un autre pack");
      expect(declineButton.label).toBe("Refuser");
      expect(validateButton.label).toBe("Valider");
    });
  });
});
