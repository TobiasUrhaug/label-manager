export const AgreementDelete = {
    confirmDelete(formId) {
        if (window.confirm('Delete this pricing agreement?')) {
            document.getElementById(formId).submit();
        }
    }
};

window.AgreementDelete = AgreementDelete;
