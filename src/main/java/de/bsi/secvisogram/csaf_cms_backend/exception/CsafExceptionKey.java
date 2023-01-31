package de.bsi.secvisogram.csaf_cms_backend.exception;

public enum CsafExceptionKey {
    AdvisoryNotFound,
    CsafHasNoDocumentNode,
    NoPermissionForAdvisory,
    InvalidDateTimeFormat,
    InvalidFilterExpression,
    UnknownExportFormat,
    ExportTimeout,
    InvalidObjectType,
    AdvisoryValidationError,
    ErrorAccessingValidationServer,
    SummaryInHistoryEmpty,
    ErrorCreatingTrackingIdCounter,
    DuplicateImport;
}
