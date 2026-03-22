import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AgreementDelete } from '../../main/resources/static/js/agreement-delete.js';

describe('AgreementDelete.confirmDelete', () => {
    let form;

    beforeEach(() => {
        form = { submit: vi.fn() };
        vi.spyOn(document, 'getElementById').mockReturnValue(form);
    });

    it('submits the form when user confirms', () => {
        vi.spyOn(window, 'confirm').mockReturnValue(true);

        AgreementDelete.confirmDelete('delete-form-1');

        expect(window.confirm).toHaveBeenCalledWith('Delete this pricing agreement?');
        expect(document.getElementById).toHaveBeenCalledWith('delete-form-1');
        expect(form.submit).toHaveBeenCalledOnce();
    });

    it('does not submit the form when user cancels', () => {
        vi.spyOn(window, 'confirm').mockReturnValue(false);

        AgreementDelete.confirmDelete('delete-form-1');

        expect(form.submit).not.toHaveBeenCalled();
    });
});
