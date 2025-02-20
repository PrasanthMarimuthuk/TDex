package com.example.tdexv01

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.tdexv01.MainActivity.Place

class VisitedPlaceDialogFragment : DialogFragment() {

    private var place: Place? = null
    private var onDateSelected: ((String) -> Unit)? = null

    companion object {
        private const val ARG_PLACE = "place"

        fun newInstance(place: Place, onDateSelected: (String) -> Unit): VisitedPlaceDialogFragment {
            val fragment = VisitedPlaceDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_PLACE, place)
            fragment.arguments = args
            fragment.onDateSelected = onDateSelected
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_visited_place, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        place = arguments?.getParcelable(ARG_PLACE)
        val dateButton = view.findViewById<Button>(R.id.btnSelectDate)
        val confirmButton = view.findViewById<Button>(R.id.btnConfirm)
        val cancelButton = view.findViewById<Button>(R.id.btnCancel)

        var selectedDate = ""

        dateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                selectedDate = dateFormat.format(calendar.time)
                dateButton.text = selectedDate
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        confirmButton.setOnClickListener {
            if (selectedDate.isNotEmpty()) {
                onDateSelected?.invoke(selectedDate)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please select a date", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }
}