import { ref } from "vue";
import { getOrgsEventsBoothPlan, getOrgsEventsPacks } from "~/utils/api";
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
      const [packsResponse, boothLocationsResponse] = await Promise.all([
        getOrgsEventsPacks(orgSlug, eventSlug),
        getOrgsEventsBoothPlan(orgSlug, eventSlug),
      ]);

      const highlightedPackById = new Map<string, HighlightedPackName>(
        packsResponse.data.flatMap((p) => (isHighlightedPack(p.name) ? [[p.id, p.name]] : [])),
      );

      if (highlightedPackById.size === 0) {
        sponsors.value = [];
        return;
      }

      const mapped: BoothSponsor[] = boothLocationsResponse.data.flatMap((item) => {
        const validatedPackId = item.partnership.validated_pack_id;
        if (!validatedPackId) return [];
        const packName = highlightedPackById.get(validatedPackId);
        if (!packName) return [];
        return [
          {
            id: item.partnership.id,
            companyName: item.partnership.company_name,
            packName,
            boothLocation: item.booth_location ?? null,
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
