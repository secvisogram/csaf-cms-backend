package de.bsi.secvisogram.csaf_cms_backend.postgres.query;

import static de.bsi.secvisogram.csaf_cms_backend.exception.CsafExceptionKey.InvalidListParameter;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import de.bsi.secvisogram.csaf_cms_backend.exception.CsafException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** One sort criterion of the paged advisory list. */
public record AdvisoryListSort(AdvisoryListSortField field, boolean descending) {

    /**
     * Parses the sort parameters of the paged list.
     *
     * @param sort criteria {@code field} or {@code field,asc|desc} (direction case-insensitive); null or empty sorts
     *             by lastUpdated, newest first
     * @return the criteria in the given order
     * @throws CsafException BAD_REQUEST for an unknown field, an unknown direction or more than one comma
     */
    public static List<AdvisoryListSort> parse(List<String> sort) throws CsafException {
        if (sort == null || sort.isEmpty()) {
            return List.of(new AdvisoryListSort(AdvisoryListSortField.LAST_UPDATED, true));
        }
        List<AdvisoryListSort> criteria = new ArrayList<>();
        for (String criterion : sort) {
            String[] parts = criterion.split(",", -1);
            Optional<AdvisoryListSortField> field = AdvisoryListSortField.fromApiName(parts[0].trim());
            String direction = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
            if (field.isEmpty() || parts.length > 2 || !(direction.equals("asc") || direction.equals("desc"))) {
                throw new CsafException("Invalid sort '" + criterion + "', expected field[,asc|desc] with field one of "
                        + Arrays.stream(AdvisoryListSortField.values()).map(AdvisoryListSortField::getApiName).toList(),
                        InvalidListParameter, BAD_REQUEST);
            }
            criteria.add(new AdvisoryListSort(field.get(), direction.equals("desc")));
        }
        return criteria;
    }
}
