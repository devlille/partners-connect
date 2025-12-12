/**
 * Composable for handling document generation (quote, invoice, agreement)
 * Centralizes error handling, state management, and success notifications
 */
export function useDocumentGeneration() {
  const toast = useCustomToast();

  /**
   * Handle document generation errors with proper formatting
   * @param err - The error object from the API call
   * @param documentType - Type of document (agreement, quote, invoice)
   * @returns Formatted error message
   */
  function handleDocumentError(err: any, documentType: "agreement" | "quote" | "invoice"): string {
    console.error(`Failed to generate ${documentType}:`, err);

    let errorMessage: string;

    // Try to extract custom error message from API response
    if (err.response?.data?.message) {
      errorMessage = err.response.data.message;
    } else if (err.response) {
      // Handle specific HTTP status codes
      switch (err.response.status) {
        case 404:
          errorMessage = "Partenariat introuvable";
          break;
        case 403:
          errorMessage = `Vous n'êtes pas autorisé à générer ${getDocumentLabel(documentType, "ce", "cette")}`;
          break;
        default:
          errorMessage = `Une erreur est survenue lors de la génération ${getDocumentLabel(documentType, "du", "de la")}`;
      }
    } else {
      // Handle network errors or unknown errors
      errorMessage = `Une erreur est survenue lors de la génération ${getDocumentLabel(documentType, "du", "de la")}`;
    }

    // Show error toast
    toast.error(errorMessage);

    return errorMessage;
  }

  /**
   * Handle successful document generation
   * @param documentId - The ID of the generated document
   * @param documentType - Type of document
   * @returns The document URL
   */
  function handleDocumentSuccess(
    documentId: string,
    documentType: "agreement" | "quote" | "invoice",
  ): string {
    // Construct document URL
    const documentUrl = `/api/documents/${documentId}`;

    // Open PDF in new tab
    window.open(documentUrl, "_blank");

    // Show success toast
    const successMessage = `${getDocumentLabel(documentType, "Le", "La", true)} a été ${documentType === "agreement" ? "générée" : "généré"} avec succès`;
    toast.success(successMessage);

    return documentUrl;
  }

  /**
   * Get the properly formatted document label with article
   * @param documentType - Type of document
   * @param masculineArticle - Article for masculine (le, du, ce)
   * @param feminineArticle - Article for feminine (la, de la, cette)
   * @param capitalize - Whether to capitalize the first letter
   * @returns Formatted document label
   */
  function getDocumentLabel(
    documentType: "agreement" | "quote" | "invoice",
    masculineArticle: string,
    feminineArticle: string,
    capitalize = false,
  ): string {
    const labels: Record<typeof documentType, { article: string; noun: string }> = {
      agreement: { article: feminineArticle, noun: "convention" },
      quote: { article: masculineArticle, noun: "devis" },
      invoice: { article: feminineArticle, noun: "facture" },
    };

    const { article, noun } = labels[documentType];
    const label = `${article} ${noun}`;

    return capitalize ? label.charAt(0).toUpperCase() + label.slice(1) : label;
  }

  /**
   * Generic wrapper for document generation API calls
   * Handles all the boilerplate: loading state, error handling, success handling
   *
   * @param generateFn - Function that calls the API to generate the document
   * @param documentType - Type of document being generated
   * @param loadingRef - Ref to track loading state
   * @param errorRef - Ref to store error message
   * @param urlRef - Ref to store document URL
   */
  async function generateDocument(
    generateFn: () => Promise<{ data: { id?: string; url?: string } }>,
    documentType: "agreement" | "quote" | "invoice",
    loadingRef: Ref<boolean>,
    errorRef: Ref<string | null>,
    urlRef: Ref<string | null>,
  ): Promise<void> {
    errorRef.value = null;
    loadingRef.value = true;

    try {
      const response = await generateFn();

      // Handle response with ID (quote/invoice)
      if (response.data.id) {
        urlRef.value = handleDocumentSuccess(response.data.id, documentType);
      }
      // Handle response with URL (agreement)
      else if (response.data.url) {
        urlRef.value = response.data.url;
        window.open(response.data.url, "_blank");
        const successMessage = `${getDocumentLabel(documentType, "Le", "La", true)} a été ${documentType === "agreement" ? "générée" : "généré"} avec succès`;
        toast.success(successMessage);
      }
      // No ID or URL in response
      else {
        errorRef.value = `Aucun ${documentType === "agreement" ? "URL de convention" : documentType === "quote" ? "ID de devis" : "ID de facture"} n'a été ${documentType === "agreement" ? "retournée" : "retourné"}`;
      }
    } catch (err: any) {
      errorRef.value = handleDocumentError(err, documentType);
    } finally {
      loadingRef.value = false;
    }
  }

  return {
    handleDocumentError,
    handleDocumentSuccess,
    generateDocument,
    getDocumentLabel,
  };
}
