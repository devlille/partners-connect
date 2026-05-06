import { ref } from "vue";
import {
  getEventsPartnershipDetailed,
  getOrgsEventsPacks,
  getOrgsEventsPartnership,
} from "~/utils/api";
import { isHighlightedPack, type HighlightedPackName } from "~/utils/booth/isHighlightedPack";

export interface BoothSponsor {
  id: string;
  companyName: string;
  packName: HighlightedPackName;
  boothLocation: string | null;
}

export function useBoothSponsors(orgSlug: string, eventSlug: string) {
  const sponsors = ref<BoothSponsor[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const errorHandler = useErrorHandler();

  function sortSponsors(items: BoothSponsor[]): BoothSponsor[] {
    return [...items].sort((a, b) => {
      if (a.boothLocation === null && b.boothLocation === null) return 0;
      if (a.boothLocation === null) return 1;
      if (b.boothLocation === null) return -1;
      return a.boothLocation.localeCompare(b.boothLocation);
    });
  }

  async function load() {
    loading.value = true;
    error.value = null;
    try {
      const [packsResponse, partnershipsResponse] = await Promise.all([
        getOrgsEventsPacks(orgSlug, eventSlug),
        getOrgsEventsPartnership(orgSlug, eventSlug, { page_size: 100 }),
      ]);

      const highlightedPackIds = new Set(
        packsResponse.data.filter((p) => isHighlightedPack(p.name)).map((p) => p.id),
      );

      if (highlightedPackIds.size === 0) {
        sponsors.value = [];
        return;
      }

      const candidatePartnerships = partnershipsResponse.data.items.filter(
        (item) => item.validated_pack_id != null && highlightedPackIds.has(item.validated_pack_id),
      );

      const detailResponses = await Promise.all(
        candidatePartnerships.map((p) => getEventsPartnershipDetailed(eventSlug, p.id)),
      );

      const mapped: BoothSponsor[] = detailResponses.flatMap((response) => {
        const partnership = response.data.partnership;
        const company = response.data.company;
        const packName = partnership.validated_pack?.name;
        if (!isHighlightedPack(packName)) return [];
        return [
          {
            id: partnership.id,
            companyName: company.name,
            packName,
            boothLocation: partnership.booth_location ?? null,
          },
        ];
      });

      sponsors.value = sortSponsors(mapped);
    } catch (err) {
      error.value = errorHandler.handleError(err, "useBoothSponsors");
      sponsors.value = [];
    } finally {
      loading.value = false;
    }
  }

  load();

  return {
    sponsors,
    loading,
    error,
    refresh: load,
  };
}
